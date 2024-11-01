package bn.poro.quran.fragments.audio_load;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import bn.poro.quran.DownloadService;
import bn.poro.quran.views.MyProgressBar;
import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;

public class AudioListAdapter extends RecyclerView.Adapter<AudioListAdapter.Holder>
        implements DownloadRunnable.DownloadProgressListener, ServiceConnection {
    public static final IsolatedItem[] isolated = {
            new IsolatedItem(R.string.single_word, "single_word_audio.zip", 132249000),
            new IsolatedItem(R.string.pair_word, "pair_word_audio.zip", 147532115)};
    private final SettingActivity activity;
    private final int[] ayahCountOfSura;
    private final String[] suraNames;
    private final int[] fileSizes;
    private DownloadService downloadService;

    AudioListAdapter(SettingActivity activity) {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select text from quran where ayah is null", null);
        ayahCountOfSura = new int[cursor.getCount()];
        for (int i = 0; i < ayahCountOfSura.length; i++) {
            cursor.moveToPosition(i);
            ayahCountOfSura[i] = cursor.getInt(0);
        }
        cursor.close();
        database.close();
        suraNames = activity.getResources().getStringArray(R.array.sura_transliteration);
        fileSizes = activity.getResources().getIntArray(R.array.size_list);
        this.activity = activity;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(viewType == 0 ? R.layout.download_item : R.layout.all_download, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (position == isolated.length) return 1;
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            DownloadRunnable task = (DownloadRunnable) payload;
            if (task.status == DownloadRunnable.STARTING) {
                holder.progressBar.setStatus(MyProgressBar.WAITING);
            } else if (task.status == DownloadRunnable.COMPLETED) {
                holder.progressBar.setStatus(MyProgressBar.COMPLETED);
            } else if (task.status == DownloadRunnable.DOWNLOADING) {
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                int size;
                if (position < isolated.length) {
                    IsolatedItem item = isolated[position];
                    size = item.size;
                    stringBuilder.append(activity.getString(isolated[position].nameRes));
                } else {
                    stringBuilder.append(Utils.formatNum(position - 2));
                    stringBuilder.append(". ");
                    stringBuilder.append(suraNames[position - 3]);
                    size = fileSizes[position - 3];
                }
                int start = stringBuilder.length();
                stringBuilder.append("\n");
                stringBuilder.append(DownloadRunnable.getString(task.totalProgress));
                stringBuilder.append("/");
                stringBuilder.append(DownloadRunnable.getString(size));
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.textView.setText(stringBuilder);
                holder.progressBar.setStatus(MyProgressBar.DOWNLOADING);
                holder.progressBar.setProgress((((float) task.totalProgress) / size) * 360);
            } else holder.progressBar.setStatus(MyProgressBar.PARTIAL_DONE);
            holder.progressBar.invalidate();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        int start;
        File file, tempFile;
        DownloadRunnable task = null;
        switch (position) {
            case 0:
            case 1:
                IsolatedItem item = isolated[position];
                stringBuilder.append(activity.getString(item.nameRes));
                stringBuilder.append("\n");
                start = stringBuilder.length();
                file = new File(Utils.dataPath + item.fileName);
                tempFile = new File(file.getPath() + Consts.LOADING_FILE);
                if (downloadService != null)
                    task = downloadService.findTask(DownloadService.TYPE_ZIP_AUDIO, position);
                if (file.exists()) holder.progressBar.setStatus(MyProgressBar.COMPLETED);
                else if (task != null) {
                    if (task.status == DownloadRunnable.DOWNLOADING)
                        holder.progressBar.setStatus(MyProgressBar.DOWNLOADING);
                    if (task.status == DownloadRunnable.STARTING)
                        holder.progressBar.setStatus(MyProgressBar.WAITING);
                } else if (tempFile.exists()) {
                    int progress = (int) tempFile.length();
                    stringBuilder.append(DownloadRunnable.getString(progress));
                    stringBuilder.append("/");
                    holder.progressBar.setStatus(MyProgressBar.PARTIAL_DONE);
                    holder.progressBar.setProgress((progress / ((float) item.size)) * 360);
                } else holder.progressBar.setStatus(MyProgressBar.NONE);
                stringBuilder.append(DownloadRunnable.getString(item.size));
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case 2:
                if (DownloadService.downloadingAllSura) {
                    holder.progressBar.setStatus(MyProgressBar.CANCEL_ALL);
                } else holder.progressBar.setStatus(MyProgressBar.NONE);
                stringBuilder.append(activity.getString(R.string.download_all));
                break;
            default:
                int suraNo = position - isolated.length;
                stringBuilder.append(Utils.formatNum(suraNo));
                stringBuilder.append(". ");
                stringBuilder.append(suraNames[suraNo - 1]);
                stringBuilder.append("\n");
                start = stringBuilder.length();
                int size = fileSizes[suraNo - 1];
                file = new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + suraNo);
                tempFile = new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + suraNo + ".zip" + Consts.LOADING_FILE);
                String[] files = file.list();
                if (downloadService != null)
                    task = downloadService.findTask(DownloadService.TYPE_SURA_AUDIO, position);
                if (files != null && files.length == ayahCountOfSura[position - 3])
                    holder.progressBar.setStatus(MyProgressBar.COMPLETED);
                else if (task != null) {
                    if (task.status == DownloadRunnable.DOWNLOADING)
                        holder.progressBar.setStatus(MyProgressBar.DOWNLOADING);
                    if (task.status == DownloadRunnable.STARTING)
                        holder.progressBar.setStatus(MyProgressBar.WAITING);
                } else if (tempFile.exists()) {
                    int progress = (int) tempFile.length();
                    holder.progressBar.setProgress((((float) progress) / size) * 360);
                    holder.progressBar.setStatus(MyProgressBar.PARTIAL_DONE);
                    stringBuilder.append(DownloadRunnable.getString(progress));
                    stringBuilder.append("/");
                } else {
                    holder.progressBar.setStatus(MyProgressBar.NONE);
                }
                stringBuilder.append(DownloadRunnable.getString(size));
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.textView.setText(stringBuilder);
    }

    @Override
    public int getItemCount() {
        return suraNames.length + isolated.length + 1;
    }

    @Override
    public void onDownloadProgress(@NonNull DownloadRunnable task) {
        if (task.type == DownloadService.TYPE_ZIP_AUDIO)
            notifyItemChanged(task.id, task);
        else if (task.type == DownloadService.TYPE_SURA_AUDIO)
            notifyItemChanged(task.id + isolated.length, task);
    }

    public void bindService() {
        activity.bindService(new Intent(activity, DownloadService.class), this, Context.BIND_ABOVE_CLIENT);
    }

    public void unbindService() {
        activity.unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        downloadService = null;
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, DialogInterface.OnClickListener {
        final TextView textView;
        final MyProgressBar progressBar;

        Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            progressBar = itemView.findViewById(R.id.progressBar);
            itemView.setOnClickListener(this);
            progressBar.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            Intent intent = new Intent(activity, DownloadService.class);
            if (position < isolated.length) {
                IsolatedItem item = isolated[position];
                intent.putExtra(Consts.TYPE_KEY, DownloadService.TYPE_ZIP_AUDIO)
                        .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + "zip/" + item.fileName)
                        .putExtra(Consts.ID_KEY, position)
                        .putExtra(Consts.PATH_KEY, Utils.dataPath + item.fileName)
                        .putExtra(Consts.NAME_KEY, activity.getString(item.nameRes));
            } else if (position == isolated.length)
                intent.putExtra(Consts.TYPE_KEY, DownloadService.TYPE_DOWNLOAD_ALL_SURA);
            else {
                int sura = position - isolated.length;
                new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura).mkdir();
                intent.putExtra(Consts.TYPE_KEY, DownloadService.TYPE_SURA_AUDIO)
                        .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura)
                        .putExtra(Consts.PATH_KEY, Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura + ".zip")
                        .putExtra(Consts.ID_KEY, sura)
                        .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + "zip/" + sura + ".zip");
            }
            switch (progressBar.status) {
                case MyProgressBar.COMPLETED:
                    if (v.getId() == R.id.progressBar) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.warning)
                                .setMessage(textView.getText())
                                .setPositiveButton(R.string.delete, this)
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    }
                    break;
                case MyProgressBar.WAITING:
                case MyProgressBar.DOWNLOADING:
                case MyProgressBar.CANCEL_ALL:
                    if (v.getId() == R.id.progressBar) {
                        activity.startService(intent);
                        progressBar.setStatus(MyProgressBar.NONE);
                    }
                    break;
                case MyProgressBar.PARTIAL_DONE:
                case MyProgressBar.NONE:
                    activity.startService(intent);
                    if (position == 2) progressBar.setStatus(MyProgressBar.CANCEL_ALL);
                    else progressBar.setStatus(MyProgressBar.WAITING);
            }
            progressBar.invalidate();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = getLayoutPosition();
            if (position < isolated.length) {
                new File(Utils.dataPath + isolated[position].fileName).delete();
            } else
                Utils.deleteFiles(new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + (position - isolated.length)));
            progressBar.setStatus(MyProgressBar.NONE);
            progressBar.invalidate();
        }
    }
}
