package bn.poro.quran.activity_quran;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;


public class QuranPlayerService extends Service implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener,
        DownloadWithoutProgress.DownloadListener, AudioManager.OnAudioFocusChangeListener {

    static final int SCROLL_TIME = 200;
    static final String ACTION_CANCEL = "0";
    static final String ACTION_PREV = "1";
    static final String ACTION_PLAY = "2";
    static final String ACTION_NEXT = "3";
    static final int PLAYER_NOTIFICATION = -1;
    static final long SURA_DELAY = 500;
    static MediaPlayer audioPlayer;
    private boolean stopAfterSura;
    private boolean wordByWord;
    private boolean playingBismillah;
    private boolean downloading;
    private boolean hasAudioFocus;
    private boolean paused;
    private int wordNumber;
    private int totalAyah;
    private int repeatCount;
    private int playCount;
    private int minutesLeftToStopPlayer;
    private int verseIntervalSeconds;
    private int playingSuraId;
    private int playingAyah;
    private boolean playingPair;
    private StopTimer timer;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder builder;
    private int[][] audioIds;
    private float playerSpeed;

    void startTimer(int min) {
        if (timer != null) timer.cancel();
        timer = new StopTimer(min);
        timer.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        stopAfterSura = store.getBoolean(Consts.STOP_SURA, false);
        repeatCount = store.getInt(Consts.REPEAT, 1);
        playerSpeed = store.getInt(Consts.PLAYER_SPEED, Consts.DEF_PLAYER_SPEED) / 10.0f;
        audioPlayer = new MediaPlayer();
        audioPlayer.setOnErrorListener(this);
        audioPlayer.setOnCompletionListener(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("player", "player", NotificationManager.IMPORTANCE_LOW);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(mChannel);
        }
        builder = new NotificationCompat.Builder(this, "player")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.ic_notif)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, QuranActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setDeleteIntent(PendingIntent.getService(this, 1,
                        new Intent(this, QuranPlayerService.class).setAction(ACTION_CANCEL),
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .addAction(R.drawable.ic_prev, "Previous", PendingIntent.getService(this, 2,
                        new Intent(this, QuranPlayerService.class).setAction(ACTION_PREV),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .addAction(R.drawable.ic_pause, "Pause", PendingIntent.getService(this, 3,
                        new Intent(this, QuranPlayerService.class).setAction(ACTION_PLAY),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .addAction(R.drawable.ic_next, "Next", PendingIntent.getService(this, 4,
                        new Intent(this, QuranPlayerService.class).setAction(ACTION_NEXT),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2));
        startForeground(PLAYER_NOTIFICATION, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action == null) {
            if (minutesLeftToStopPlayer == 0) minutesLeftToStopPlayer = Integer.MAX_VALUE;
            playingSuraId = intent.getIntExtra(Consts.EXTRA_SURA_ID, 0);
            playingAyah = intent.getIntExtra(Consts.EXTRA_AYAH_NUM, 1);
            wordByWord = intent.getBooleanExtra(Consts.SHOW_BY_WORD, false);
            audioPlayer.stop();
            L.d("play command: " + playingSuraId + "," + playingAyah);
            paused = false;
            playCount = 1;
            initSura();
        } else switch (action) {
            case "0":
                stop();
                break;
            case "1":
                playPrev();
                break;
            case "2":
                playPause();
                break;
            case "3":
                if (wordByWord) playingPair = true;
                playNext();
                break;
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("RestrictedApi")
    void playPause() {
        NotificationCompat.Action action = builder.mActions.get(1);
        if (audioPlayer.isPlaying()) {
            audioPlayer.pause();
            if (hasAudioFocus) {
                requestFocus(false);
                hasAudioFocus = false;
            }
            paused = true;
            builder.setOngoing(false);
            builder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play, action.title, action.actionIntent));
            if (QuranActivity.playerListener != null)
                QuranActivity.playerListener.onPause();
        } else {
            if (hasAudioFocus || requestFocus(true))
                audioPlayer.start();
            paused = false;
            builder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause, action.title, action.actionIntent));
            builder.setOngoing(true);
            if (QuranActivity.playerListener != null)
                QuranActivity.playerListener.onPlay();
        }
        notificationManager.notify(PLAYER_NOTIFICATION, builder.build());
    }

    private void initSura() {
        if (playingSuraId > Consts.SURA_COUNT) {
            stop();
            return;
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select text from quran where sura=? and ayah is null",
                new String[]{String.valueOf(playingSuraId - 1)});
        cursor.moveToFirst();
        totalAyah = cursor.getInt(0);
        builder.setContentTitle(getResources().getStringArray(R.array.sura_transliteration)[playingSuraId - 1]);
        cursor.close();
        database.close();
        if (playingAyah == 0) playingAyah = totalAyah;
        if (playingSuraId != 1 && playingSuraId != 9 && playingAyah == 1) {
            playingBismillah = true;
            play(1, 1);
        } else {
            if (wordByWord) initAyah();
            else play(playingSuraId, playingAyah);
        }
    }

    void stop() {
        if (QuranActivity.playerListener != null) {
            if (wordByWord) QuranActivity.playerListener.onPlay(wordNumber, false);
            if (playingPair) QuranActivity.playerListener.onPlay(wordNumber - 1, false);
            QuranActivity.playerListener.onStop();
        }
        if (hasAudioFocus) requestFocus(false);
        stopSelf();
        L.d("playerService stopSelf");
    }

    @Override
    public void onDestroy() {
        audioPlayer.stop();
        audioPlayer.release();
        notificationManager.cancel(PLAYER_NOTIFICATION);
        audioPlayer = null;
        super.onDestroy();
        L.d("playerService destroy");
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (minutesLeftToStopPlayer > 0)
            if (!playingBismillah && playCount < repeatCount) {
                playCount++;
                mediaPlayer.start();
            } else {
                playNext();
            }
        else stop();
    }

    @SuppressLint("RestrictedApi")
    void playPrev() {
        playCount = 1;
        if (wordByWord) {
            if (wordNumber <= 1) audioPlayer.seekTo(0);
            else {
                if (QuranActivity.playerListener != null)
                    QuranActivity.playerListener.onPlay(wordNumber, false);
                wordNumber -= 2;
                playingPair = true;
                playNext();
            }
        } else {
            audioPlayer.pause();
            playingBismillah = false;
            if (--playingAyah == 0) {
                if (playingSuraId == 1) {
                    audioPlayer.start();
                    playPause();
                    audioPlayer.seekTo(0);
                    playingAyah = 1;
                } else {
                    playingSuraId--;
                    initSura();
                }
            } else play(playingSuraId, playingAyah);
        }
    }

    void playNext() {
        playCount = 1;
        audioPlayer.pause();
        if (playingBismillah) {
            playingBismillah = false;
            if (wordByWord) initAyah();
            else play(playingSuraId, playingAyah);
        } else {
            if (wordByWord) playNextWord();
            else playNextAyah();
        }
    }

    private void playNextAyah() {
        if (playingAyah == totalAyah) {
            if (stopAfterSura) stop();
            else {
                playingSuraId++;
                playingAyah = 1;
                new Handler(Looper.getMainLooper()).postDelayed(this::initSura, SURA_DELAY);
            }
        } else {
            playingAyah++;
            play(playingSuraId, playingAyah);
        }
    }

    private void playNextWord() {
        if (!playingPair) {
            int pair = audioIds[wordNumber - 1][1];
            if (pair != 0) {
                playingPair = true;
                builder.setContentText(getString(R.string.quran_ayah) + ": " + Utils.formatNum(playingAyah) + " " + "W: " + Utils.formatNum(wordNumber - 1) +
                        " + " + Utils.formatNum(wordNumber));
                notificationManager.notify(PLAYER_NOTIFICATION, builder.build());
                if (QuranActivity.playerListener != null)
                    QuranActivity.playerListener.onPlay(wordNumber - 1, true);
                WordClickHandler.playWord(audioPlayer, pair, this);
                return;
            }
        } else {
            if (QuranActivity.playerListener != null && wordNumber != 0)
                QuranActivity.playerListener.onPlay(wordNumber - 1, false);
            playingPair = false;
        }
        if (audioIds.length > wordNumber) {
            wordNumber++;
            builder.setContentText(getString(R.string.quran_ayah) + ": " + Utils.formatNum(playingAyah) + " " + "W: " + Utils.formatNum(wordNumber));
            notificationManager.notify(PLAYER_NOTIFICATION, builder.build());
            if (QuranActivity.playerListener != null) {
                QuranActivity.playerListener.onPlay(wordNumber, true);
                if (wordNumber > 1)
                    QuranActivity.playerListener.onPlay(wordNumber - 1, false);
            }
            WordClickHandler.playWord(audioPlayer, audioIds[wordNumber - 1][0], this);
        } else if (playingAyah < totalAyah) {
            playingAyah++;
            initAyah();
        } else {
            if (stopAfterSura) stop();
            else {
                playingAyah = 1;
                playingSuraId++;
                new Handler(Looper.getMainLooper()).postDelayed(this::initSura, SURA_DELAY);
            }
        }
    }

    private void initAyah() {
        SQLiteDatabase corpus = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = corpus.rawQuery("select a,b from audio where id=(select rowid from quran where sura=? and ayah=?)",
                new String[]{String.valueOf(playingSuraId - 1), String.valueOf(playingAyah)});
        audioIds = new int[cursor.getCount()][2];
        for (int i = 0; i < audioIds.length; i++) {
            cursor.moveToPosition(i);
            audioIds[i][0] = cursor.getInt(0);
            audioIds[i][1] = cursor.getInt(1);
        }
        cursor.close();
        wordNumber = 1;
        builder.setContentText(getString(R.string.quran_ayah) + ": " + Utils.formatNum(playingAyah) + " " + "W: " + Utils.formatNum(wordNumber));
        notificationManager.notify(PLAYER_NOTIFICATION, builder.build());
        if (QuranActivity.playerListener != null) {
            QuranActivity.playerListener.onPlay(playingSuraId, playingAyah);
            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    QuranActivity.playerListener.onPlay(1, true), SCROLL_TIME);
        }
        WordClickHandler.playWord(audioPlayer, audioIds[0][0], this);
    }

    private void play(int sura, int ayah) {
        String path = Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura + File.separator + ayah + ".mp3";
        File file = new File(path);
        if (file.exists()) {
            play(path);
        } else {
            if (!downloading) {
                new DownloadWithoutProgress(file, DownloadService.BASE_URL + Consts.QURAN_AUDIO_SUB_PATH + sura + File.separator + ayah + ".mp3", this).start();
                downloading = true;
            }
            if (QuranActivity.playerListener != null)
                QuranActivity.playerListener.onDownloadStart();
            notificationManager.cancel(PLAYER_NOTIFICATION);
        }
    }

    private void play(String path) {
        if (QuranActivity.playerListener != null)
            QuranActivity.playerListener.onPlay(playingSuraId, playingAyah);
        audioPlayer.stop();
        audioPlayer.reset();
        try {
            audioPlayer.setDataSource(path);
            audioPlayer.prepare();
            setSpeed();
            if ((hasAudioFocus || requestFocus(true)) && !paused) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> audioPlayer.start(), verseIntervalSeconds * SECOND_IN_MILLIS);
            }
        } catch (Exception e) {
            L.d(e);
            return;
        }
        if (wordByWord) return;
        builder.setContentText(getString(R.string.quran_ayah) + ": " + Utils.formatNum(playingAyah));
        notificationManager.notify(PLAYER_NOTIFICATION, builder.build());
        int nextSura, nextAyah;
        if (playingAyah == totalAyah) {
            if (playingSuraId == Consts.SURA_COUNT) return;
            nextAyah = 1;
            nextSura = 1;
        } else {
            nextSura = playingSuraId;
            if (playingBismillah) nextAyah = playingAyah;
            else nextAyah = playingAyah + 1;
        }
        String filePath = Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + nextSura + File.separator + nextAyah + ".mp3";
        File file = new File(filePath);
        if (!file.exists()) {
            new DownloadWithoutProgress(file, DownloadService.BASE_URL + Consts.QURAN_AUDIO_SUB_PATH + nextSura + File.separator + nextAyah + ".mp3", this).start();
            downloading = true;
        }
    }

    private boolean requestFocus(boolean request) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager == null) return hasAudioFocus = true;
        if (request)
            return hasAudioFocus = audioManager.requestAudioFocus(this, 3, AudioManager.AUDIOFOCUS_GAIN)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        audioManager.abandonAudioFocus(this);
        return false;
    }

    @Override
    public void onDownloaded(File file, int code) {
        if (code == DownloadRunnable.COMPLETED) {
            if (audioPlayer != null && !audioPlayer.isPlaying() && !paused) {
                play(file.getPath());
                if (QuranActivity.playerListener != null)
                    QuranActivity.playerListener.onDownloadEnd();
            }
        } else if (playingPair || playingBismillah) {
            if (audioPlayer != null && !audioPlayer.isPlaying() && !paused) {
                playNext();
                if (QuranActivity.playerListener != null)
                    QuranActivity.playerListener.onDownloadEnd();
            }
        } else {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            if (audioPlayer != null && !audioPlayer.isPlaying()) {
                stop();
            }
            if (QuranActivity.playerListener != null) QuranActivity.playerListener.onDownloadEnd();
        }
        downloading = false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        hasAudioFocus = focusChange == AudioManager.AUDIOFOCUS_GAIN;
        if (audioPlayer != null) playPause();
    }

    public void setRepeat(int c) {
        repeatCount = c;
    }

    void setDelay(int i) {
        verseIntervalSeconds = i;
    }

    public void setPair(boolean b) {
        playingPair = b;
    }

    public boolean isStopAfterSura() {
        return stopAfterSura;
    }

    public void stopAfterSura(boolean isChecked) {
        stopAfterSura = isChecked;
    }

    public boolean playingWordByWord() {
        return wordByWord;
    }

    public int verseDelay() {
        return verseIntervalSeconds;
    }

    public int remainingTime() {
        return minutesLeftToStopPlayer;
    }

    public int playingSuraIndex() {
        return playingSuraId - 1;
    }

    int playingAyah() {
        return playingAyah;
    }

    @SuppressLint("NewApi")
    public void setSpeed() {
        try {
            audioPlayer.setPlaybackParams(audioPlayer.getPlaybackParams().setSpeed(playerSpeed));
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("NewApi")
    public void setPlayerSpeed(float playerSpeed) {
        this.playerSpeed = playerSpeed;
        setSpeed();
    }

    public interface PlayerListener {
        void onPlay(int word, boolean press);

        void onMinuteElapse(int time);

        void onDownloadEnd();

        void onDownloadStart();

        void onStop();

        void onPlay(int sura, int ayah);

        void onPlay(int pos, int word, boolean press);

        void onPlay();

        void onPause();
    }

    private class StopTimer extends CountDownTimer {
        static final long SECOND = 60000L;

        public StopTimer(int min) {
            super(min * SECOND, SECOND);
            minutesLeftToStopPlayer = min + 1;
        }

        @Override
        public void onTick(long millisUntilFinished) {
            minutesLeftToStopPlayer--;
            if (QuranActivity.playerListener != null)
                QuranActivity.playerListener.onMinuteElapse(minutesLeftToStopPlayer);
        }

        @Override
        public void onFinish() {
            minutesLeftToStopPlayer = 0;
        }
    }

    class LocalBinder extends Binder {
        QuranPlayerService getService() {
            return QuranPlayerService.this;
        }
    }
}
