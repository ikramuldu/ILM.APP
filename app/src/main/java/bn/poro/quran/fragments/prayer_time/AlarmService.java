package bn.poro.quran.fragments.prayer_time;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.util.Collection;
import java.util.Date;
import java.util.regex.Pattern;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;

public class AlarmService extends Service {

    public static final int PRE_ALARM_OFFSET = 100;
    static final String ACTION_STOP_ALARM = "stop_alarm";
    private static final String ALARM_CHANNEL_ID = "alarm";
    private static final int NOTIFICATION_ID = -99;
    private static final int FULL_SCREEN_REQUEST = 1;
    private static final String ACTION_DISMISS_ALARM = "dismiss_alarm";
    private static final String ACTION_REMOVE_ALARM = "remove_alarm";
    private static final String POSITION_KEY = "pos";
    private Ringtone ringtone;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(ALARM_CHANNEL_ID,
                    ALARM_CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public void onDestroy() {
        if (ringtone != null) {
            ringtone.stop();
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.cancel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.d("started alarm service: " + startId);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        SharedPreferences pref = getSharedPreferences(Consts.ALARM_PREFS, Context.MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        String action = intent.getAction();
        Intent activityIntent = new Intent(this, AlarmActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Pattern.matches("\\d+", action)) {
            int id = Integer.parseInt(action);
            PendingIntent pendingBroadcast = PendingIntent.getBroadcast(this, id, new Intent(this, MyReceiver.class).setAction(action), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            if (pendingBroadcast != null) {
                alarmManager.cancel(pendingBroadcast);
                pendingBroadcast.cancel();
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALARM_CHANNEL_ID);
            builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm);
            String[] names = getResources().getStringArray(R.array.prayerNames);
            if (id >= PRE_ALARM_OFFSET) {
                long alarmTime = pref.getLong(action, System.currentTimeMillis() + HOUR_IN_MILLIS);
                int alarmId = id - PRE_ALARM_OFFSET;
                builder.setContentText(names[alarmId] + " alarm at " + PrayerTimeAdapter.formatAMPM(this, alarmTime));
                builder.setWhen(alarmTime);
                builder.setContentTitle("Upcoming Alarm");
                builder.setPriority(NotificationCompat.PRIORITY_LOW);
                builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                builder.addAction(R.drawable.ic_close, "cancel today", PendingIntent.getService(this, id, new Intent(this, AlarmService.class).setAction(ACTION_DISMISS_ALARM).putExtra(POSITION_KEY, alarmId), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
                manager.notify(NOTIFICATION_ID, builder.build());
                return START_NOT_STICKY;
            }
            String name = names[id];
            activityIntent.putExtra(Consts.NAME_KEY, name);
            PendingIntent pendingActivity = PendingIntent.getActivity(this, FULL_SCREEN_REQUEST, activityIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.setFullScreenIntent(pendingActivity, true);
            builder.setContentIntent(pendingActivity);
            PendingIntent stopAlarm = PendingIntent.getService(this, id, new Intent(this, AlarmService.class).setAction(ACTION_STOP_ALARM), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
            builder.setDeleteIntent(stopAlarm);
            RemoteViews view = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.notification);
            view.setCharSequence(R.id.text, "setText", name);
            view.setOnClickPendingIntent(R.id.stop, stopAlarm);
            builder.setCustomContentView(view);
            builder.setCustomHeadsUpContentView(view);
            builder.setCategory(NotificationCompat.CATEGORY_ALARM);
            manager.notify(NOTIFICATION_ID, builder.build());
            long[] vibratePattern = new long[]{500, 500};
            vibrator.vibrate(vibratePattern, 0);
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            ringtone.setAudioAttributes(new AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_ALARM).build());
            ringtone.play();
        } else if (ACTION_STOP_ALARM.equals(action) || Intent.ACTION_SCREEN_OFF.equalsIgnoreCase(action)) {
            PendingIntent pendingActivity = PendingIntent.getActivity(this, FULL_SCREEN_REQUEST, activityIntent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pendingActivity != null) pendingActivity.cancel();
            manager.cancel(NOTIFICATION_ID);
            if (ringtone != null)
                ringtone.stop();
            vibrator.cancel();
            ringtone = null;
            stopSelf();
        } else if (ACTION_DISMISS_ALARM.equals(action)) {
            manager.cancel(NOTIFICATION_ID);
            int position = intent.getIntExtra(POSITION_KEY, 0);
            Intent alarmIntent = new Intent(this, MyReceiver.class).setAction(String.valueOf(position));
            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(this, position,
                    alarmIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            if (alarmPendingIntent != null) {
                alarmManager.cancel(alarmPendingIntent);
                alarmPendingIntent.cancel();
            }
            int preAlarmId = position + PRE_ALARM_OFFSET;
            String preAlarmAction = String.valueOf(preAlarmId);
            long alarmTime = pref.getLong(preAlarmAction, 0) + DAY_IN_MILLIS;

            alarmPendingIntent = PendingIntent.getBroadcast(this, position, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTime, alarmPendingIntent), alarmPendingIntent);

            PendingIntent preAlarmPendingIntent = PendingIntent.getBroadcast(this, preAlarmId, new Intent(this, MyReceiver.class).setAction(preAlarmAction), PendingIntent.FLAG_IMMUTABLE);
            L.d("setting alarm:" + PrayerTimeAdapter.formatAMPM(this, alarmTime));
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTime - HOUR_IN_MILLIS, preAlarmPendingIntent), preAlarmPendingIntent);
            stopSelf();
        } else
            resetAlarms(this, true);
        return START_NOT_STICKY;
    }

    static void resetAlarms(Context context, boolean resetAll) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
            return;
        SharedPreferences preferences = context.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        SharedPreferences alarmPrefs = context.getSharedPreferences(Consts.ALARM_PREFS, Context.MODE_PRIVATE);
        int latitude = preferences.getInt(Consts.LATITUDE, Consts.LATITUDE_DHAKA);
        int longitude = preferences.getInt(Consts.LONGITUDE, Consts.LONGITUDE_DHAKA);
        int convention = preferences.getInt(Consts.CONVENTION, 4);
        Calculator calculator = new Calculator(latitude, longitude, new Date(), convention);
        Collection<Integer> alarmData = Utils.readAlarmData(context);
        for (int position : alarmData) {
            String alarmAction = String.valueOf(position);
            Intent alarmIntent = new Intent(context, MyReceiver.class).setAction(alarmAction);
            int preAlarmId = position + PRE_ALARM_OFFSET;
            String preAlarmAction = String.valueOf(preAlarmId);
            Intent preAlarmIntent = new Intent(context, MyReceiver.class).setAction(preAlarmAction);
            PendingIntent preAlarmPendingIntent = PendingIntent.getBroadcast(context, preAlarmId, preAlarmIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            int offset = alarmPrefs.getInt(alarmAction, 0);
            long oldAlarmTime = alarmPrefs.getLong(preAlarmAction, 0);
            long alarmTime = calculator.startOf(position) + offset * MINUTE_IN_MILLIS;
            long currentTime = System.currentTimeMillis();
            while (alarmTime < currentTime) alarmTime += DAY_IN_MILLIS;
            while (alarmTime > currentTime + DAY_IN_MILLIS) alarmTime -= DAY_IN_MILLIS;
            if (alarmTime - currentTime < 3 * MINUTE_IN_MILLIS) alarmTime += DAY_IN_MILLIS;
            boolean override = resetAll || (Math.abs(alarmTime - oldAlarmTime) > MINUTE_IN_MILLIS);
            if (preAlarmPendingIntent != null && override) {
                alarmManager.cancel(preAlarmPendingIntent);
                preAlarmPendingIntent.cancel();
            }
            long preAlarmTime = alarmTime - HOUR_IN_MILLIS;
            if (preAlarmPendingIntent == null || override) {
                preAlarmPendingIntent = PendingIntent.getBroadcast(context, preAlarmId, preAlarmIntent, PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(preAlarmTime, preAlarmPendingIntent), preAlarmPendingIntent);
            }
            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, position, alarmIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
            if (alarmPendingIntent != null && override) {
                alarmManager.cancel(alarmPendingIntent);
                alarmPendingIntent.cancel();
            }
            if (alarmPendingIntent == null || override) {
                alarmPendingIntent = PendingIntent.getBroadcast(context, position, alarmIntent, PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTime, alarmPendingIntent), alarmPendingIntent);
                alarmPrefs.edit().putLong(preAlarmAction, alarmTime).apply();
            }
        }
    }
}
