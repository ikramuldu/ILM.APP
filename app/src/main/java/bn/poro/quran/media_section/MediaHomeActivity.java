package bn.poro.quran.media_section;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.Locale;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_setting.SettingActivity;

public class MediaHomeActivity extends AppCompatActivity implements
        View.OnClickListener,
        SurfaceHolder.Callback,
        DownloadWithoutProgress.DownloadListener,
        View.OnTouchListener,
        SeekBar.OnSeekBarChangeListener, ServiceConnection {
    static String lang;
    private final int[] navIDs = {R.id.book, R.id.article, R.id.video, R.id.audio, R.id.fatawa};
    private int screenY, screenX, currentPage, prevTouchTime, backward_forward;
    int sortBy;
    private boolean fullscreen;
    private boolean landscape;
    private boolean fitScreen;
    private boolean keepRatio;
    private AlertDialog dialog;
    private PlayerService playerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.ISLAMHOUSE_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        lang = MainActivity.getAppLang();
        sortBy = store.getInt(Consts.ISLAMHOUSE_SORT_KEY, R.id.a_z);
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.media_main);
        View view = findViewById(R.id.drawer_layout);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        View nav = view.findViewById(R.id.bottom_nav);
        for (int id : navIDs) {
            nav.findViewById(id).setOnClickListener(this);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(metrics);
        if (metrics.widthPixels > metrics.heightPixels) {
            landscape = true;
            screenX = metrics.heightPixels;
            screenY = metrics.widthPixels;
        } else {
            landscape = false;
            screenX = metrics.widthPixels;
            screenY = metrics.heightPixels;
        }

        view.findViewById(R.id.media_frame).setOnTouchListener(this);
        SeekBar seekBar = view.findViewById(R.id.media_seek);
        seekBar.setOnSeekBarChangeListener(this);
        ImageView playButton = view.findViewById(R.id.play);
        playButton.setOnClickListener(this);
        view.findViewById(R.id.full).setOnClickListener(this);
        SurfaceView videoView = view.findViewById(R.id.video_surface);
        videoView.getHolder().addCallback(this);
        if (new File(Utils.dataPath + lang + ".db").exists()) {
            initView();
        } else {
            dialog = new AlertDialog.Builder(this).setView(R.layout.loading).setCancelable(false).show();
            new DownloadWithoutProgress(new File(Utils.dataPath + lang + ".db"), DownloadService.BASE_URL + "lang/" + lang + ".zip", this).start();
        }
    }

    private void initView() {
        ViewPager2 viewPager = findViewById(R.id.view_pager2);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new PageAdapter(this));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setCurrentPage(position);
            }
        });
        viewPager.setCurrentItem(2);
        SharedPreferences preferences = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(Consts.ISLAMHOUSE_NOTIFY, true)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.note);
            builder.show();
            preferences.edit().putBoolean(Consts.ISLAMHOUSE_NOTIFY, false).apply();
        }
    }

    @Override
    public void onDownloaded(File file, int code) {
        dialog.dismiss();
        if (code == DownloadRunnable.COMPLETED) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("alter table data add column size INTEGER;");
            database.close();
            initView();
        } else Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        landscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (playerService != null && playerService.mediaPlayer.getVideoWidth() != 0) {
            if (fullscreen) goFullScreen();
            else goNormal();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) finish();
        else if (id == R.id.short_by) {
            SubMenu subMenu = item.getSubMenu();
            if (subMenu != null) {
                MenuItem menuItem = subMenu.findItem(sortBy);
                if (menuItem != null) menuItem.setChecked(true);
            }
        } else {
            sortBy = id;
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(Consts.ISLAMHOUSE_SORT_KEY, id).apply();
            item.setChecked(true);
            ViewPager2 viewPager = findViewById(R.id.view_pager2);
            viewPager.setAdapter(new PageAdapter(this));
        }
        return true;
    }

    private void goFullScreen() {
        int videoWidth = playerService.mediaPlayer.getVideoWidth();
        int videoHeight = playerService.mediaPlayer.getVideoHeight();
        if (videoHeight == 0 || videoWidth == 0) return;
        fullscreen = true;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 0x00000004 | 0x00001000);
        findViewById(R.id.media_frame).getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        ((ImageView) findViewById(R.id.full)).setImageResource(R.drawable.exit_full);
        ViewGroup.LayoutParams video = findViewById(R.id.video_surface).getLayoutParams();
        if (!fitScreen) {
            video.height = videoHeight;
            video.width = videoWidth;
            return;
        }
        if (!keepRatio) {
            video.height = ViewGroup.LayoutParams.MATCH_PARENT;
            video.width = ViewGroup.LayoutParams.MATCH_PARENT;
            return;
        }
        int height, screenHeight;
        if (landscape) {
            height = (screenY * videoHeight) / videoWidth;
            screenHeight = screenX;
        } else {
            height = (screenX * videoHeight) / videoWidth;
            screenHeight = screenY;
        }

        if (height <= screenHeight) {
            video.width = ViewGroup.LayoutParams.MATCH_PARENT;
            video.height = height;
        } else {
            video.height = screenHeight;
            video.width = (screenHeight * videoWidth) / videoHeight;
        }
    }

    private void goNormal() {
        fullscreen = false;
        getWindow().getDecorView().setSystemUiVisibility(0x00000100);
        ((ImageView) findViewById(R.id.full)).setImageResource(R.drawable.full);
        findViewById(R.id.media_frame).getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        ViewGroup.LayoutParams video = findViewById(R.id.video_surface).getLayoutParams();
        int videoWidth = playerService.mediaPlayer.getVideoWidth();
        int videoHeight = playerService.mediaPlayer.getVideoHeight();
        int calculatedHeight, maxHeight;
        if (landscape) {
            calculatedHeight = (videoHeight * screenY) / videoWidth;
            maxHeight = screenX >> 1;
        } else {
            calculatedHeight = (videoHeight * screenX) / videoWidth;
            maxHeight = screenY >> 1;
        }
        if (!fitScreen) {
            if (videoHeight < maxHeight) {
                video.height = videoHeight;
                video.width = videoWidth;
            } else {
                video.height = maxHeight;
                if (keepRatio) video.width = (maxHeight * videoWidth) / videoHeight;
                else video.width = videoWidth;
            }
        } else if (calculatedHeight < maxHeight) {
            video.height = calculatedHeight;
            video.width = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            video.height = maxHeight;
            if (keepRatio) video.width = (maxHeight * videoWidth) / videoHeight;
            else video.width = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    static String getTime(int time) {
        if (time < 3600) return String.format(Locale.US, "%02d:%02d", time / 60, time % 60);
        else {
            int hr = time / 3600;
            time %= 3600;
            return String.format(Locale.US, "%d:%02d:%02d", hr, time / 60, time % 60);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0 && playerService.mediaPlayer != null) {
            int dif = (int) (System.nanoTime() / 100000000) - prevTouchTime;
            prevTouchTime = prevTouchTime + dif;
            View controller = findViewById(R.id.media_control);
            if (dif <= 5) {
                int pos = playerService.mediaPlayer.getCurrentPosition();
                int x = (int) motionEvent.getRawX();
                int w = landscape ? screenY : screenX;
                w /= 5;
                if (x < w << 1) {
                    playerService.mediaPlayer.seekTo(pos - backward_forward * 1000);
                } else if (x > w * 3) {
                    playerService.mediaPlayer.seekTo(pos + backward_forward * 1000);
                } else {
                    startService(new Intent(this, PlayerService.class)
                            .putExtra(Consts.MEDIA_ACTION_KEY, Consts.MEDIA_ACTION_PAUSE));
                }
                controller.setVisibility(View.VISIBLE);
            } else if (controller.getVisibility() == View.VISIBLE) {
                controller.setVisibility(View.GONE);
            } else {
                controller.setVisibility(View.VISIBLE);
            }
        }
        return true;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean fromUser) {
        if (fromUser) {
            playerService.mediaPlayer.seekTo(i * 1000);
        }
        ((TextView) findViewById(R.id.time)).setText(getTime(i));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (RecreateManager.needRecreate(RecreateManager.ISLAMHOUSE_ACTIVITY))
            recreate();
        else {
            if (PlayerService.isRunning)
                bindService(new Intent(this, PlayerService.class), this, Context.BIND_ABOVE_CLIENT);
            fitScreen = store.getBoolean(Consts.FIT_SCREEN_KEY, true);
            keepRatio = store.getBoolean(Consts.KEEP_RATIO_KEY, true);
            backward_forward = store.getInt(Consts.BACKWARD_KEY, 10);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playerService = ((PlayerService.Binder) service).getService();
        playerService.setListener(this);
        View view = findViewById(R.id.drawer_layout);
        if (playerService.mediaPlayer.getVideoWidth() != 0) {
            view.findViewById(R.id.video_surface).setVisibility(View.VISIBLE);
            if (fullscreen) goFullScreen();
            else goNormal();
        }
        View controller = view.findViewById(R.id.media_control);
        controller.setVisibility(View.VISIBLE);
        TextView fullTime = controller.findViewById(R.id.full_time);
        int duration = playerService.mediaPlayer.getDuration() / 1000;
        ImageView playButton = view.findViewById(R.id.play);
        SeekBar seekBar = view.findViewById(R.id.media_seek);
        seekBar.setMax(duration);
        if (playerService.mediaPlayer.isPlaying())
            playButton.setImageResource(R.drawable.ic_pause);
        fullTime.setText(getTime(duration));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playerService = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (playerService != null)
            unbindService(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.full) {
            if (fullscreen) {
                goNormal();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            } else {
                goFullScreen();
                int videoWidth = playerService.mediaPlayer.getVideoWidth();
                int videoHeight = playerService.mediaPlayer.getVideoHeight();
                if (videoHeight < videoWidth) {
                    if (fitScreen || videoWidth > screenX)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    if (fitScreen || videoHeight > screenX)
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
            return;
        }
        if (id == R.id.play) {
            startService(new Intent(this, PlayerService.class)
                    .putExtra(Consts.MEDIA_ACTION_KEY, Consts.MEDIA_ACTION_PAUSE));
            return;
        }

        int page;
        for (page = 0; page < 5; page++) {
            if (id == navIDs[page]) break;
        }
        setCurrentPage(page);
        ((ViewPager2) findViewById(R.id.view_pager2)).setCurrentItem(page, true);
    }

    private void setCurrentPage(int page) {
        if (page != currentPage) {
            findViewById(navIDs[currentPage]).setSelected(false);
            findViewById(navIDs[page]).setSelected(true);
            currentPage = page;
        }
    }

    public void onPrepared(MediaPlayer mediaPlayer) {
        int time = mediaPlayer.getDuration() / 1000;
        ((TextView) findViewById(R.id.full_time)).setText(getTime(time));
        ((SeekBar) findViewById(R.id.media_seek)).setMax(time);
        findViewById(R.id.loading).setVisibility(View.GONE);
        LinearLayout controller = findViewById(R.id.media_control);
        controller.setVisibility(View.VISIBLE);
        ((ImageView) controller.findViewById(R.id.play)).setImageResource(R.drawable.ic_pause);
        if (mediaPlayer.getVideoWidth() == 0) {
            controller.findViewById(R.id.full).setVisibility(View.GONE);
            findViewById(R.id.video_surface).setVisibility(View.GONE);
            findViewById(R.id.media_frame).getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            controller.findViewById(R.id.full).setVisibility(View.VISIBLE);
            findViewById(R.id.video_surface).setVisibility(View.VISIBLE);
            goNormal();
        }
    }

    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        ((SeekBar) findViewById(R.id.media_seek)).setSecondaryProgress((mediaPlayer.getDuration() * i) / 100000);
    }


    void toggleState(int icon) {
        ((ImageView) findViewById(R.id.play)).setImageResource(icon);
    }

    void updateTime(int time) {
        View loading = findViewById(R.id.loading);
        if (time == 0) loading.setVisibility(View.VISIBLE);
        else {
            loading.setVisibility(View.GONE);
            ((SeekBar) findViewById(R.id.media_seek)).setProgress(time);

        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        playerService.mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        playerService.mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        playerService.mediaPlayer.setDisplay(null);
    }
}