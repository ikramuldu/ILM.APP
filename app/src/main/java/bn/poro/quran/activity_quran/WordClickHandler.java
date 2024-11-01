package bn.poro.quran.activity_quran;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.fragments.audio_load.AudioListAdapter;
import bn.poro.quran.views.FontSpan;

public class WordClickHandler extends ClickableSpan implements
        DialogInterface.OnCancelListener,
        DownloadWithoutProgress.DownloadListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        View.OnClickListener,
        View.OnLongClickListener {
    private static final int RESUME_DELAY = 200;
    private static final long RIPPLE_DELAY = 200;
    private static boolean continuePlay;
    private static int imageStart;
    private final Typeface arabicFont;
    private MediaPlayer audioPlayer;
    private int wordId;
    private final Activity activity;
    TextView textView;
    private int pairWord;
    private int ayahIndex;
    private int wordIndex;
    private boolean isPaused;

    public WordClickHandler(Activity activity, Typeface arabicFont) {
        this.activity = activity;
        this.arabicFont = arabicFont;
        if (audioPlayer == null) {
            audioPlayer = new MediaPlayer();
            audioPlayer.setOnErrorListener(this);
            audioPlayer.setOnCompletionListener(this);
        }
    }

    @Override
    public void onClick(@NonNull View view) {
        if (view.getTag() != null) wordId = (int) view.getTag();
        if (textView != null) {
            SpannableString stringBuilder = new SpannableString(textView.getText());
            if (continuePlay) {
                continuePlay = false;
                stringBuilder.setSpan(new ImageSpan(activity, android.R.drawable.ic_media_play, DynamicDrawableSpan.ALIGN_BASELINE),
                        imageStart - 2, imageStart - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return;
            } else {
                continuePlay = true;
                stringBuilder.setSpan(new ImageSpan(activity, android.R.drawable.ic_media_pause, DynamicDrawableSpan.ALIGN_BASELINE),
                        imageStart - 2, imageStart - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor wordCursor = database.rawQuery("select a,b from audio where rowid=? limit 1", new String[]{String.valueOf(wordId)});
        if (wordCursor.getCount() == 0) return;
        wordCursor.moveToFirst();
        int audio = wordCursor.getInt(0);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> playWord(audioPlayer, audio, this), RESUME_DELAY);

        if (QuranPlayerService.audioPlayer != null && QuranPlayerService.audioPlayer.isPlaying()) {
            QuranPlayerService.audioPlayer.pause();
            isPaused = true;
            if (QuranActivity.playerListener != null)
                QuranActivity.playerListener.onPlay();
        }
        if (activity instanceof QuranActivity) {
            pairWord = wordCursor.getInt(1);
            try {
                ViewGroup wordGroup = (ViewGroup) view.getParent();
                AyahItem item = (AyahItem) ((View) wordGroup.getParent()).getTag();
                ayahIndex = ((QuranActivity) activity).getPosition(item);
                wordIndex = wordGroup.indexOfChild(view);
                handler.postDelayed(() -> QuranActivity.playerListener.onPlay(ayahIndex,
                        wordIndex + 1, true), RIPPLE_DELAY);
            } catch (Exception e) {
                L.d(e);
            }
        }
        wordCursor.close();
    }

    public static void playWord(MediaPlayer audioPlayer, int wordId, DownloadWithoutProgress.DownloadListener downloadListener) {
        String fileName = wordId + ".mp3";
        File tempFile = new File(Utils.dataPath + Consts.TEMP_WORD_DIR + fileName);
        if (!tempFile.exists()) {//no cache
            File zFile;
            if (wordId < 40000)
                zFile = new File(Utils.dataPath + AudioListAdapter.isolated[0].fileName);
            else zFile = new File(Utils.dataPath + AudioListAdapter.isolated[1].fileName);
            if (zFile.exists()) try {//extracting Zip
                ZipFile zipFile = new ZipFile(zFile);
                ZipEntry zipEntry = zipFile.getEntry(fileName);
                if (zipEntry != null) {
                    Utils.copy(zipFile.getInputStream(zipEntry), tempFile);
                }
                zipFile.close();
            } catch (Exception e) {
                L.d(e);
            }
        }
        if (tempFile.exists()) {
            audioPlayer.stop();
            audioPlayer.reset();
            try {
                audioPlayer.setDataSource(tempFile.getPath());
                audioPlayer.prepare();
            } catch (IOException e) {
                tempFile.delete();
                return;
            } catch (Exception e) {
                L.d(e);
                return;
            }
            audioPlayer.start();
        } else {
            String url = DownloadService.BASE_URL + "w/" + fileName;
            new DownloadWithoutProgress(tempFile, url, downloadListener).start();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getTag() != null) wordId = (int) view.getTag();
        textView = (TextView) activity.getLayoutInflater().inflate(R.layout.long_click_text, null);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor wordCursor = database.rawQuery("select ar,root from corpus where word=?", new String[]{String.valueOf(wordId)});
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        StringBuilder plusSeparatedParts = new StringBuilder();
        while (wordCursor.moveToNext()) {
            String part = wordCursor.getString(0);
            stringBuilder.append(part);
            if (plusSeparatedParts.length() > 0)
                plusSeparatedParts.append(" + ");
            plusSeparatedParts.append(part);
        }
        stringBuilder.setSpan(new RelativeSizeSpan(2f), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (arabicFont != null)
            stringBuilder.setSpan(new FontSpan(arabicFont), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.append("   ");
        imageStart = stringBuilder.length();
        stringBuilder.setSpan(new ImageSpan(activity, android.R.drawable.ic_media_play, DynamicDrawableSpan.ALIGN_BASELINE),
                imageStart - 2, imageStart - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(this, imageStart - 3, imageStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (wordCursor.getCount() > 1) {
            stringBuilder.append("\n");
            stringBuilder.append(plusSeparatedParts);
        }
        textView.setHighlightColor(Color.TRANSPARENT);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(stringBuilder);
        wordCursor.close();
        new AlertDialog.Builder(activity)
                .setView(textView)
                .setOnCancelListener(this)
                .show();
        return true;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        textView = null;
        continuePlay = false;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (continuePlay) new Handler().postDelayed(() -> {
            if (continuePlay) mp.start();
        }, 1000);
        else if (pairWord > 0 && QuranActivity.playerListener != null) {
            playWord(audioPlayer, pairWord, this);
            QuranActivity.playerListener.onPlay(ayahIndex, wordIndex, true);
            pairWord = -1;
        } else {
            if (QuranActivity.playerListener != null) {
                if (pairWord == -1) {
                    pairWord = 0;
                    QuranActivity.playerListener.onPlay(ayahIndex, wordIndex, false);
                }
                QuranActivity.playerListener.onPlay(ayahIndex, wordIndex + 1, false);
            }
            if (isPaused && QuranPlayerService.audioPlayer != null) {
                new Handler().postDelayed(() -> {
                    int pos = QuranPlayerService.audioPlayer.getCurrentPosition();
                    QuranPlayerService.audioPlayer.seekTo(pos > SECOND_IN_MILLIS ? (int) (pos - SECOND_IN_MILLIS) : 0);
                    QuranPlayerService.audioPlayer.start();
                    isPaused = false;
                    if (QuranActivity.playerListener != null)
                        QuranActivity.playerListener.onPause();
                }, RESUME_DELAY);
            }
        }
    }

    @Override
    public void onDownloaded(File file, int code) {
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
            if (textView != null) {
                continuePlay = false;
                Spannable spannable = (Spannable) textView.getText();
                spannable.setSpan(new ImageSpan(activity, android.R.drawable.ic_media_play),
                        imageStart - 2, imageStart - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (QuranActivity.playerListener != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> QuranActivity.playerListener.onPlay(ayahIndex, wordIndex + 1, false), RIPPLE_DELAY);
            }
            Toast.makeText(activity, R.string.error_network, Toast.LENGTH_SHORT).show();
        }
    }
}
