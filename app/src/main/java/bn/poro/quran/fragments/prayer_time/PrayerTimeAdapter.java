package bn.poro.quran.fragments.prayer_time;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;

class PrayerTimeAdapter extends RecyclerView.Adapter<PrayerTimeAdapter.Holder> implements TimePicker.OnTimeChangedListener, TextView.OnEditorActionListener, View.OnKeyListener {
    public static final int NO_POSITION = -1;
    public static final int FAJR_POSITION = 0;
    public static final int SUNRISE_POSITION = 1;
    public static final int ISHRAK_POSITION = 2;
    public static final int ZAWAAL_POSITION = 3;
    public static final int JUHR_POSITION = 4;
    public static final int ASR_POSITION = 5;
    public static final int SUNSET_POSITION = 6;
    public static final int MAGRIB_POSITION = 7;
    public static final int ISHA_POSITION = 8;
    public static final int SUHR_POSITION = 9;
    public static final long MAX_LEVEL = 10000;
    public static final int UPDATE_ALL = -1;
    public static final int REMOVE_HIGHLIGHT = -2;
    public static final int ADD_BORDER = -3;
    private static final int HOUR2MINUTE = 60;
    final MainActivity activity;
    final String[] names;
    final int[] colors = new int[2];
    int activePosition = NO_POSITION;
    boolean glowBorder;
    long onlyAsrTime, magribTime, suhrEndTime, juhrTime, jawalTime, ishrakTime, fazrTime, midday, midnight, lastNightTime, ishaTime, asrOther, asrHanafi, sunriseTime, sunsetTime;
    private ArrayList<Integer> alarmData;
    private long startTime;
    private int offsetMinutes;
    private AlertDialog alertDialog;


    PrayerTimeAdapter(MainActivity activity, Date date) {
        this.activity = activity;
        names = activity.getResources().getStringArray(R.array.prayerNames);
        TypedArray array = activity.obtainStyledAttributes(new int[]{R.attr.colorError,
                android.R.attr.textColor});
        colors[0] = array.getColor(0, Color.BLACK);
        colors[1] = array.getColor(1, Color.BLACK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            array.close();
        } else array.recycle();
        refresh(date);
        new Handler(Looper.getMainLooper()).post(() -> AlarmService.resetAlarms(activity, false));
    }

    public void refresh(Date date) {
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        int latitude = preferences.getInt(Consts.LATITUDE, Consts.LATITUDE_DHAKA);
        int longitude = preferences.getInt(Consts.LONGITUDE, Consts.LONGITUDE_DHAKA);
        int convention = preferences.getInt(Consts.CONVENTION, 4);
        alarmData = Utils.readAlarmData(activity);
        Calculator calculator = new Calculator(latitude, longitude, date, convention);
        if (date.getDay() == 5) {
            names[4] = activity.getString(R.string.jumma);
        } else names[4] = activity.getString(R.string.juhr);
        sunriseTime = calculator.sunriseTime();
        fazrTime = calculator.fazrTime(sunriseTime);
        midday = calculator.juhrTime();
        asrOther = calculator.asrTime(1);
        asrHanafi = calculator.asrTime(2);
        sunsetTime = calculator.magribTime();
        ishaTime = calculator.ishaTime(sunsetTime);
        midnight = calculator.midnight(sunsetTime);
        lastNightTime = calculator.lastNightTime(sunsetTime);
        sunriseTime = Calculator.prevMinute(sunriseTime);
        sunsetTime = Calculator.nextMinute(sunsetTime);
        ishrakTime = sunriseTime + PrayerTimeFragment.SUNRISE_SUNSET_TIME + MINUTE_IN_MILLIS;
        jawalTime = midday - PrayerTimeFragment.MIDDAY_CAUTION_TIME;
        juhrTime = midday + PrayerTimeFragment.MIDDAY_CAUTION_TIME + MINUTE_IN_MILLIS;
        suhrEndTime = fazrTime - PrayerTimeFragment.SUHR_CAUTION_TIME;
        magribTime = sunsetTime + PrayerTimeFragment.MAGRIB_CAUTION_TIME;
        onlyAsrTime = sunsetTime - MINUTE_IN_MILLIS - PrayerTimeFragment.SUNRISE_SUNSET_TIME;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.item_time, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof Boolean) {
                holder.alarmButton.setImageResource((boolean) payload ? R.drawable.ic_time : R.drawable.ic_bell);
                continue;
            }
            int level = (int) payload;
            MaterialCardView cardView = (MaterialCardView) holder.itemView;
            switch (level) {
                case UPDATE_ALL:
                    setTime(holder.timeText, position);
                    break;
                case REMOVE_HIGHLIGHT:
                    holder.layout.getBackground().setLevel(0);
                    cardView.setStrokeWidth(0);
                    if (cardView.getTag() instanceof ObjectAnimator) {
                        ((ObjectAnimator) cardView.getTag()).cancel();
                        cardView.setTag(null);
                    }
                    break;
                case ADD_BORDER:
                    cardView.setStrokeWidth(5);
                    if (glowBorder) glowBorder(cardView);
                    break;
                default:
                    holder.layout.getBackground().setLevel(level);
                    break;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        setTime(holder.timeText, position);
        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        if (position == activePosition) {
            cardView.setStrokeWidth(5);
            if (glowBorder) glowBorder(cardView);
        } else {
            cardView.setStrokeWidth(0);
            holder.layout.getBackground().setLevel(0);
            if (cardView.getTag() instanceof ObjectAnimator) {
                ((ObjectAnimator) cardView.getTag()).cancel();
                cardView.setTag(null);
            }
        }
        int colorIndex = position == 1 || position == 3 || position == 6 ? 0 : 1;
        holder.timeText.setTextColor(colors[colorIndex]);
        holder.nameText.setTextColor(colors[colorIndex]);
        holder.nameText.setText(names[position]);
        holder.alarmButton.setImageResource(isAlarmSet(position) ? R.drawable.ic_time : R.drawable.ic_bell);
    }

    private boolean isAlarmSet(int position) {
        return alarmData.contains(position);
    }

    private void glowBorder(MaterialCardView cardView) {
        TypedArray array = activity.obtainStyledAttributes(new int[]{R.attr.active,
                R.attr.colorAccent});
        final ObjectAnimator animator = ObjectAnimator.ofObject(cardView,
                "strokeColor",
                new ArgbEvaluator(),
                array.getColor(0, Color.BLUE),
                array.getColor(1, Color.TRANSPARENT));
        cardView.setTag(animator);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            array.close();
        } else array.recycle();
        animator.setDuration(600);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();

    }

    @SuppressLint("SetTextI18n")
    private void setTime(TextView timeText, int position) {
        switch (position) {
            case FAJR_POSITION:
                timeText.setText(formatTime(fazrTime) + " - " +
                        formatTime(sunriseTime - MINUTE_IN_MILLIS));
                break;
            case SUNRISE_POSITION:
                timeText.setText(formatTime(sunriseTime) + " - " +
                        formatTime(ishrakTime - MINUTE_IN_MILLIS));
                break;
            case ISHRAK_POSITION:
                timeText.setText(formatTime(ishrakTime) + " - " +
                        formatTime(jawalTime - MINUTE_IN_MILLIS));
                break;
            case ZAWAAL_POSITION:
                timeText.setText(formatTime(jawalTime) + " - " +
                        formatTime(juhrTime - MINUTE_IN_MILLIS));
                break;
            case JUHR_POSITION:
                timeText.setText(formatTime(juhrTime) + " - " +
                        formatTime(asrHanafi - MINUTE_IN_MILLIS));
                break;
            case ASR_POSITION:
                timeText.setText(formatTime(asrHanafi) + " - " +
                        formatTime(sunsetTime - MINUTE_IN_MILLIS));
                break;
            case SUNSET_POSITION:
                timeText.setText(formatTime(onlyAsrTime) + " - " +
                        formatTime(sunsetTime - MINUTE_IN_MILLIS));
                break;
            case MAGRIB_POSITION:
                timeText.setText(formatTime(magribTime) + " - " +
                        formatTime(ishaTime - MINUTE_IN_MILLIS));
                break;
            case ISHA_POSITION:
                timeText.setText(formatTime(ishaTime) + " - " +
                        formatTime(midnight - MINUTE_IN_MILLIS));
                break;
            case SUHR_POSITION:
                timeText.setText(formatTime(lastNightTime) + " - " +
                        formatTime(suhrEndTime - MINUTE_IN_MILLIS));
                break;
        }
    }

    String formatTime(long time) {
        return new SimpleDateFormat(activity.getString(DateFormat.is24HourFormat(activity) ?
                R.string.hour_format_24hours : R.string.hour_format_12hours), MainActivity.getLocale()).format(time);
    }

    static String formatAMPM(Context context, long time) {
        return new SimpleDateFormat(BuildConfig.DEBUG ? "dd hh:mm a" : context.getString(DateFormat.is24HourFormat(context) ?
                R.string.hour_format_24hours : R.string.hour_format_12_am), MainActivity.getLocale()).format(time);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        Date date = new Date(startTime);
        offsetMinutes = minute - date.getMinutes() + (hourOfDay - date.getHours()) * HOUR2MINUTE;
        EditText textView = (EditText) view.getTag();
        String s = String.valueOf(offsetMinutes);
        textView.setText(s);
        textView.setSelection(s.length());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            v.clearFocus();
        }
        return false;
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) return false;
        try {
            offsetMinutes = Integer.parseInt(((TextView) v).getText().toString());
        } catch (Exception e) {
            offsetMinutes = 0;
        }
        Date date = new Date(startTime + offsetMinutes * MINUTE_IN_MILLIS);
        TimePicker timePicker = (TimePicker) v.getTag();
        timePicker.setOnTimeChangedListener(null);
        timePicker.setCurrentHour(date.getHours());
        timePicker.setCurrentMinute(date.getMinutes());
        timePicker.setOnTimeChangedListener(this);
        return false;
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView nameText;
        final TextView timeText;
        final View layout;
        final ImageView alarmButton;

        public Holder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text);
            timeText = itemView.findViewById(R.id.time);
            layout = itemView.findViewById(R.id.layout);
            itemView.setOnClickListener(this);
            alarmButton = itemView.findViewById(R.id.alarm_button);
            alarmButton.setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            int vid = v.getId();
            boolean exists = alarmData.contains(position);
            if (vid == R.id.set) {
                if (exists) removeAlarm(position);
                else {
                    alarmData.add(position);
                    notifyItemChanged(position, alarmData.contains(position));
                    Utils.saveAlarmData(activity, alarmData);
                }
                TimePicker timePicker = alertDialog.findViewById(R.id.time_picker);
                Date date = new Date(startTime);
                if (timePicker != null)
                    offsetMinutes = timePicker.getCurrentMinute() - date.getMinutes() + (timePicker.getCurrentHour() - date.getHours()) * HOUR2MINUTE;
                setAlarm(position);
                if (alertDialog != null) alertDialog.dismiss();
                return;
            }
            if (vid == R.id.delete) {
                if (exists) {
                    alarmData.remove((Integer) position);
                    removeAlarm(position);
                    notifyItemChanged(position, alarmData.contains(position));
                    Utils.saveAlarmData(activity, alarmData);
                }
                if (alertDialog != null) alertDialog.dismiss();
                return;
            }
            if (vid == R.id.alarm_button) {
                if (activity.launchAlarmPermission()) return;
                if (activity.launchFullscreenAlarm()) return;
                View view = LayoutInflater.from(activity).inflate(R.layout.alarm_dialog, null);
                EditText editText = view.findViewById(R.id.edit);
                offsetMinutes = activity.getSharedPreferences(Consts.ALARM_PREFS, Context.MODE_PRIVATE).getInt(String.valueOf(position), 0);
                editText.setOnEditorActionListener(PrayerTimeAdapter.this);
                TextView textView = view.findViewById(R.id.title);
                startTime = startOf(position);
                textView.setText(names[position] + activity.getString(R.string.start) + formatAMPM(activity, startTime));
                TimePicker timePicker = view.findViewById(R.id.time_picker);
                Date date = new Date(startTime + offsetMinutes * MINUTE_IN_MILLIS);
                timePicker.setTag(editText);
                editText.setTag(timePicker);
                editText.setOnKeyListener(PrayerTimeAdapter.this);
                timePicker.setCurrentMinute(date.getMinutes());
                timePicker.setOnTimeChangedListener(PrayerTimeAdapter.this);
                timePicker.setCurrentHour(date.getHours());
                View removeButton = view.findViewById(R.id.delete);
                if (alarmData.contains(position))
                    removeButton.setOnClickListener(this);
                else removeButton.setVisibility(View.GONE);
                view.findViewById(R.id.set).setOnClickListener(this);
                alertDialog = new AlertDialog.Builder(activity).setView(view).show();
                return;
            }
            String msg = null;
            switch (position) {
                case 0:
                    msg = activity.getString(R.string.fajr_hint);
                    break;
                case 1:
                    msg = activity.getString(R.string.sunrise_hint);
                    break;
                case 2:
                    msg = activity.getString(R.string.duha_hint);
                    break;
                case 3:
                    msg = activity.getString(R.string.midday_hint);
                    break;
                case 4:
                    msg = activity.getString(R.string.juhr_hint,
                            formatTime(midday), formatTime(asrHanafi - MINUTE_IN_MILLIS), formatTime(asrOther - MINUTE_IN_MILLIS));
                    break;
                case 5:
                    msg = activity.getString(R.string.asr_hint, formatTime(asrHanafi), formatTime(asrOther));
                    break;
                case 6:
                    msg = activity.getString(R.string.sunset_hint);
                    break;
                case 7:
                    msg = activity.getString(R.string.magrib_hint, formatTime(sunsetTime));
                    break;
                case 8:
                    msg = activity.getString(R.string.isha_hint, formatTime(fazrTime - 4 * MINUTE_IN_MILLIS),
                            formatTime(midnight));
                    break;
                case 9:
                    msg = activity.getString(R.string.suhr_hint);
                    break;
            }
            new AlertDialog.Builder(activity).setTitle(nameText.getText())
                    .setPositiveButton(R.string.ok, null)
                    .setMessage(msg).show();
        }
    }

    private long startOf(int position) {
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        int latitude = preferences.getInt(Consts.LATITUDE, Consts.LATITUDE_DHAKA);
        int longitude = preferences.getInt(Consts.LONGITUDE, Consts.LONGITUDE_DHAKA);
        int convention = preferences.getInt(Consts.CONVENTION, 4);
        return new Calculator(latitude, longitude, new Date(), convention).startOf(position);
    }

    private void removeAlarm(int position) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, position, new Intent(activity, MyReceiver.class).setAction(String.valueOf(position)), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        int preAlarmAction = position + AlarmService.PRE_ALARM_OFFSET;
        PendingIntent preAlarm = PendingIntent.getBroadcast(activity, preAlarmAction, new Intent(activity, MyReceiver.class).setAction(String.valueOf(preAlarmAction)), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        if (preAlarm == null) L.e("PrayerTimeAdapter: removeAlarm: preAlarm is null");
        else {
            alarmManager.cancel(preAlarm);
            preAlarm.cancel();
        }
        if (pendingIntent == null) L.e("PrayerTimeAdapter: removeAlarm: AlarmIntent is null");
        else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void setAlarm(int position) {
        AlarmManager alarmManager = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences pref = activity.getSharedPreferences(Consts.ALARM_PREFS, Context.MODE_PRIVATE);
        long time = startTime + offsetMinutes * MINUTE_IN_MILLIS;
        while (time < System.currentTimeMillis()) time += DAY_IN_MILLIS;
        while (time > System.currentTimeMillis() + DAY_IN_MILLIS) time -= DAY_IN_MILLIS;
        Toast.makeText(activity, "Alarm set at " + formatAMPM(activity, time), Toast.LENGTH_SHORT).show();
        String alarmAction = String.valueOf(position);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(activity, position, new Intent(activity, MyReceiver.class).setAction(
                alarmAction), PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(time, alarmIntent), alarmIntent);
        int prePosition = position + AlarmService.PRE_ALARM_OFFSET;
        String preAlarmAction = String.valueOf(prePosition);
        PendingIntent preAlarm = PendingIntent.getBroadcast(activity, prePosition, new Intent(activity, MyReceiver.class).setAction(preAlarmAction), PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(time - HOUR_IN_MILLIS, preAlarm), preAlarm);
        pref.edit().putInt(alarmAction, offsetMinutes)
                .putLong(preAlarmAction, time).apply();
    }
}
