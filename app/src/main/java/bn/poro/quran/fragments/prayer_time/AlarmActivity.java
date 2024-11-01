package bn.poro.quran.fragments.prayer_time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class AlarmActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        int themeID = store.getInt(Consts.THEME_KEY, 0);
        setTheme(SettingActivity.THEME[themeID]);
        setContentView(R.layout.alarm_activity);
        turnScreenOn();
        findViewById(R.id.stop).setOnClickListener(this);
        TextView textView = findViewById(R.id.text);
        textView.setText(getIntent().getStringExtra(Consts.NAME_KEY));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stopAlarm();
                unregisterReceiver(this);
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        L.d("new intent");
        if (AlarmService.ACTION_STOP_ALARM.equals(intent.getAction())) {
            stopAlarm();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        L.d(keyCode + event.toString());
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) {
            stopAlarm();
            return true;
        }
        return false;
    }

    private void turnScreenOn() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    public void onClick(View v) {
        stopAlarm();
    }

    private void stopAlarm() {
        try {
            startService(new Intent(this, AlarmService.class).setAction(AlarmService.ACTION_STOP_ALARM));
        } catch (Exception e) {
            L.d(e);
        }
        finish();
    }
}
