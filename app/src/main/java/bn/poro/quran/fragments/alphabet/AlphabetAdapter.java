package bn.poro.quran.fragments.alphabet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_quran.WordClickHandler;
import bn.poro.quran.views.FontSpan;

class AlphabetAdapter extends RecyclerView.Adapter<AlphabetAdapter.Holder> implements DownloadWithoutProgress.DownloadListener, DownloadRunnable.DownloadProgressListener, ServiceConnection {
    private final int banglaFontSize, arabicFontSize;
    private Typeface arabicFont;
    private final Typeface banglaFont;
    final MediaPlayer audioPlayer;
    final Activity activity;
    private String[][] dataList;
    private final String table;
    private AlertDialog alertDialog;

    AlphabetAdapter(Activity activity, String table) {
        this.activity = activity;
        this.table = table;
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        banglaFontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC);
        int fontId = store.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        banglaFont = ResourcesCompat.getFont(activity, R.font.kalpurush);
        try {
            retry();
        } catch (Exception exception) {
            try {
                Utils.copyFromAssets(activity, Consts.ARABIC_DB);
            } catch (Exception e) {
                L.d(e);
            }
            retry();
        }
        AlphabetFragment.spanCount = Integer.parseInt(dataList[0][1]);
        audioPlayer = new MediaPlayer();
    }

    private void retry() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from " + table, null);
        int columnCount = cursor.getColumnCount();
        dataList = new String[cursor.getCount()][columnCount];
        for (int i = 0; i < dataList.length; i++) {
            cursor.moveToPosition(i);
            for (int j = 0; j < columnCount; j++) dataList[i][j] = cursor.getString(j);
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(viewType == 0 ? R.layout.textview : R.layout.latter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SpannableString spannableString = new SpannableString(dataList[position][0]);
        if (position == 0) {
            if (holder.textView.getText().length() > 0) return;
            Matcher matcher = Pattern.compile("[\u0600-\u06ff][\u0600-\u06ff ]*").matcher(spannableString);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (arabicFont != null)
                    spannableString.setSpan(new FontSpan(arabicFont), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (table.equals("alphabet") && position > 4) {//alphabet color
            int col = 3 - (position - 1) % 4;
            if (col == 0) {
                col = spannableString.length();
                spannableString.setSpan(new RelativeSizeSpan(0.6f), 0, col - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new FontSpan(banglaFont), 0, col - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            spannableString.setSpan(new ForegroundColorSpan(Consts.tajweed[6]), col - 1, col, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (dataList[position].length == 3 && dataList[position][2] != null) {
            String[] spans = dataList[position][2].split("\n");
            for (String span : spans) {
                String[] pos = span.split(",");
                int start = Integer.parseInt(pos[0]);
                int end = Integer.parseInt(pos[1]);
                int spanType;
                if (pos.length >= 3) spanType = Integer.parseInt(pos[2]);
                else spanType = 6;
                if (spanType == 9) {
                    spannableString.setSpan(new PlayAudioOnCLick(this, pos[3]), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    spannableString.setSpan(new ForegroundColorSpan(Consts.tajweed[spanType]), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        holder.textView.setText(spannableString);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 0;
        return 1;
    }

    @Override
    public int getItemCount() {
        return dataList.length;
    }

    @Override
    public void onDownloaded(File file, int code) {
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        if (!preferences.contains(Consts.LEARNING_AUDIO_DOWNLOADED)) {
            new AlertDialog.Builder(activity).setMessage("Do you want to download all audio files. (5.88 MB)").setNegativeButton("No", null).setPositiveButton(R.string.ok, (dialog, which) -> download()).show();
            preferences.edit().putBoolean(Consts.LEARNING_AUDIO_DOWNLOADED, false).apply();
            activity.invalidateOptionsMenu();
        }
        if (code == DownloadRunnable.COMPLETED) {
            try {
                audioPlayer.stop();
                audioPlayer.reset();
                audioPlayer.setDataSource(file.getPath());
                audioPlayer.prepare();
                audioPlayer.start();
            } catch (Exception e) {
                L.d(e);
            }
        } else {
            Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
        }
    }

    public void download() {
        Intent intent = new Intent(activity, DownloadService.class)
                .putExtra(Consts.ID_KEY, 0)
                .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_LEARNING_AUDIO)
                .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + "zip/learning_audio.zip")
                .putExtra(Consts.PATH_KEY, Utils.dataPath + "learning_audio.zip")
                .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.TEMP_WORD_DIR)
                .putExtra(Consts.NAME_KEY, "Audio Files");
        activity.bindService(intent, this, Context.BIND_ABOVE_CLIENT);
        activity.startService(intent);
        alertDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.downloading_title)
                .setMessage("0.0%")
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onDownloadProgress(DownloadRunnable task) {
        if (task.status == DownloadRunnable.COMPLETED) {
            alertDialog.dismiss();
            activity.invalidateOptionsMenu();
        } else if (task.status == DownloadRunnable.DOWNLOAD_FAILED) {
            alertDialog.setMessage(activity.getString(R.string.error_network));
        } else
            alertDialog.setMessage(String.format("%.1f%%", (((float) task.totalProgress) / task.totalSize) * 100));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DownloadService downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;

        Holder(@NonNull View itemView) {
            super(itemView);
            if (itemView.getId() == R.id.text) {
                textView = (TextView) itemView;
                textView.setTextSize(banglaFontSize);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                textView = itemView.findViewById(R.id.text);
                textView.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            play(dataList[getLayoutPosition()][1]);
        }
    }

    public void play(String id) {
        WordClickHandler.playWord(audioPlayer, Integer.parseInt(id), this);
    }
}
