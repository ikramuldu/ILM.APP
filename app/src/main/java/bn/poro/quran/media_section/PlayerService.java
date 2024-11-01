package bn.poro.quran.media_section;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;


public class PlayerService extends Service implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnPreparedListener {

    String mediaLength;
    MediaPlayer mediaPlayer;
    MediaHomeActivity islamhouseMain;
    static boolean isRunning;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private MyTimer timer;
    private int shownTime;
    private String title;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        isRunning = true;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
        timer = new MyTimer();
        timer.start();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("player", "player", NotificationManager.IMPORTANCE_LOW);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(mChannel);
        }
        notificationBuilder = new NotificationCompat.Builder(this, "player");
        notificationBuilder.setSmallIcon(R.drawable.ic_notif)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_app));
        notificationBuilder.setOngoing(true);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationBuilder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MediaHomeActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        notificationBuilder.addAction(R.drawable.ic_pause, "Pause",
                PendingIntent.getService(this, 1,
                        new Intent(this, PlayerService.class)
                                .putExtra(Consts.ACTION_KEY, Consts.MEDIA_ACTION_PAUSE),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0));
        startForeground(Consts.PLAYER_NOTIFICATION, notificationBuilder.build());
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getIntExtra(Consts.ACTION_KEY, Consts.ACTION_PREPARE)) {
            case Consts.ACTION_PREPARE:
                if (title != null) {
                    getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(title, mediaPlayer.getCurrentPosition()).apply();
                }
                title = intent.getStringExtra(Consts.NAME_KEY);
                notificationBuilder.setContentTitle(title);
                notificationBuilder.setContentText("loading");
                shownTime = 0;
                mediaPlayer.stop();
                mediaPlayer.reset();
                String path = intent.getStringExtra(Consts.MEDIA_PATH_KEY);
                if (path != null)
                    try {
                        mediaPlayer.setDataSource(path);
                        mediaPlayer.prepareAsync();
                        Bitmap bitmap = BitmapFactory.decodeFile(getCacheDir().getPath()
                                + path.substring(path.lastIndexOf('/')));
                        if (bitmap != null)
                            notificationBuilder.setLargeIcon(bitmap);
                    } catch (Exception e) {
                        L.d(e);
                    }
                break;
            case Consts.MEDIA_ACTION_PAUSE:
                if (startId == 1) {
                    notificationManager.cancel(Consts.PLAYER_NOTIFICATION);
                    return START_NOT_STICKY;
                }
                NotificationCompat.Action action = notificationBuilder.mActions.get(0);
                int icon;
                if (mediaPlayer.isPlaying()) {
                    getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(title, mediaPlayer.getCurrentPosition()).apply();
                    mediaPlayer.pause();
                    notificationBuilder.setOngoing(false);
                    icon = R.drawable.ic_play;
                    notificationBuilder.mActions.set(0, new NotificationCompat.Action(icon, "Play", action.actionIntent));
                } else {
                    mediaPlayer.start();
                    notificationBuilder.setOngoing(true);
                    icon = R.drawable.ic_pause;
                    notificationBuilder.mActions.set(0, new NotificationCompat.Action(icon, "Pause", action.actionIntent));
                }
                if (islamhouseMain != null)
                    islamhouseMain.toggleState(icon);
                break;
        }
        notificationManager.notify(Consts.PLAYER_NOTIFICATION, notificationBuilder.build());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(title, mediaPlayer.getCurrentPosition()).apply();
        mediaPlayer.release();
        notificationManager.cancel(Consts.PLAYER_NOTIFICATION);
        mediaLength = null;
        mediaPlayer = null;
        timer = null;
        notificationManager = null;
        notificationBuilder = null;
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        notificationManager.cancel(Consts.PLAYER_NOTIFICATION);
        if (islamhouseMain != null)
            islamhouseMain.toggleState(R.drawable.ic_play);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (islamhouseMain != null)
            islamhouseMain.onBufferingUpdate(mp, percent);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.seekTo(getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).getInt(title, 0));
        mp.start();
        int time = mp.getDuration() / 1000;
        mediaLength = " / " + MediaHomeActivity.getTime(time);
        if (islamhouseMain != null) islamhouseMain.onPrepared(mp);
    }

    void setListener(MediaHomeActivity islamhouseMain) {
        this.islamhouseMain = islamhouseMain;
    }

    private class MyTimer extends CountDownTimer {
        MyTimer() {
            super(50000000, 1000);
        }

        @Override
        public void onTick(long l) {
            if (mediaPlayer.isPlaying()) {
                int time = mediaPlayer.getCurrentPosition();
                int dif = time - shownTime;
                if (dif > 500 || dif < -500) {
                    shownTime = time;
                    time /= 1000;
                    notificationBuilder.setContentText(MediaHomeActivity.getTime(time) + mediaLength);
                    notificationManager.notify(Consts.PLAYER_NOTIFICATION, notificationBuilder.build());
                } else time = 0;
                if (islamhouseMain != null)
                    islamhouseMain.updateTime(time);
            }
        }

        @Override
        public void onFinish() {

        }
    }

    class Binder extends android.os.Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }
}
