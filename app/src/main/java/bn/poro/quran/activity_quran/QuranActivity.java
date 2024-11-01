package bn.poro.quran.activity_quran;

import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_search.SearchQuranActivity;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.print.PrintActivity;

public class QuranActivity extends AppCompatActivity implements ServiceConnection, DialogInterface.OnClickListener, View.OnClickListener, TextView.OnEditorActionListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {

    public static final String WORD_SPACE = "  ";
    static QuranPlayerService.PlayerListener playerListener;
    QuranPlayerService playerService;
    int playingAyahBackColor;
    private float scrollTime;
    int tabTextColor;
    int banglaColumn = -1;
    int currentSuraIndex;
    int playingPosition = RecyclerView.NO_POSITION;
    int wordScrolledOffset;
    float banglaFontSize, arabicFontSize;
    public Typeface arabicFont;
    Typeface kalpurush, quranBangla;
    Matcher qalqala, iqlab, idgham, ikhfa, ghunna;
    private boolean justifyTrans;
    private boolean scrollWithPlayer;
    boolean isAutoScrolling;
    private boolean userIsDragging;
    private boolean fullQuranView;
    private boolean rtl;
    Stack<TextView> viewPool;
    WordClickHandler clickHandler;
    View playerController;
    private AlertDialog alertDialog;
    private FullQuranAdapter fullQuranAdapter;
    private ViewPager2 viewPager2;
    int fontId;
    ArrayList<Integer> wordDBIds;
    SparseArray<String> translationNames;
    static ArrayList<SuraInfo> allSuraInfo;
    int highlightWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.QURAN_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.activity_quran);
        playerController = findViewById(R.id.controller);
        playerController.post(this::initView);
    }

    private void initView() {
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (allSuraInfo == null) {
            String[] names = getResources().getStringArray(R.array.sura_transliteration);
            SQLiteDatabase quranDatabase = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor suraCursor = quranDatabase.rawQuery("select rowid,text,word from quran where ayah is null", null);
            allSuraInfo = new ArrayList<>(suraCursor.getCount());
            while (suraCursor.moveToNext())
                allSuraInfo.add(new SuraInfo(suraCursor.getInt(0), suraCursor.getInt(1), suraCursor.getInt(2), names[suraCursor.getPosition()]));
            suraCursor.close();
            quranDatabase.close();
        }
        float density = getResources().getDisplayMetrics().scaledDensity;
        fullQuranView = store.getBoolean(Consts.FULL_QURAN_VIEW, false);
        rtl = store.getBoolean(Consts.SURA_R2L, true);
        boolean showByWord = store.getBoolean(Consts.SHOW_BY_WORD, true);
        boolean showTrans = store.getBoolean(Consts.SHOW_TRANS, true);
        boolean showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        justifyTrans = store.getBoolean(Consts.JUSTIFICATION, false);
        if (showTrans) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("ATTACH DATABASE ? AS sts", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
            Cursor cursor = database.rawQuery("select id,lang||'- '||name from trans " + "inner join status using (id) where extra=1 order by id=5203 desc,id=5202 desc", null);
            int count = cursor.getCount();
            if (count == 0) return;
            translationNames = new SparseArray<>(count);
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                String name = cursor.getString(1);
                int id = cursor.getInt(0);
                if (id == Consts.BENGALI_TAJWEED_TRANSLITERATION_ID)
                    quranBangla = ResourcesCompat.getFont(this, R.font.banglaquran);
                translationNames.append(id, name);
            }
            cursor.close();
            database.close();
        }
        if (showArabic && showByWord) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select id from status where id<? and extra=1 order by id=5003 desc,id=5002 desc", new String[]{String.valueOf(DownloadService.TRANS_START_INDEX)});
            int count = cursor.getCount();
            if (count == 0) return;
            wordDBIds = new ArrayList<>(count);
            while (cursor.moveToNext()) wordDBIds.add(cursor.getInt(0));
            banglaColumn = wordDBIds.indexOf(DownloadService.WORD_START_INDEX);
            cursor.close();
            database.close();
        }
        banglaFontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        fontId = store.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0) arabicFont = ResourcesCompat.getFont(this, Consts.FONT_LIST[fontId]);
        kalpurush = ResourcesCompat.getFont(this, R.font.kalpurush);
        scrollWithPlayer = store.getBoolean(Consts.AUTO_SCROLL, true);
        scrollTime = store.getFloat(Consts.SCROLL_SPEED, Consts.DEF_SCROLL_TIME);
        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.tab_text, R.attr.inactive});
        tabTextColor = typedArray.getColor(0, 0);
        playingAyahBackColor = typedArray.getColor(1, 0);
        typedArray.recycle();
        if (showArabic) {
            viewPool = new Stack<>();
            clickHandler = new WordClickHandler(this, arabicFont);
        }
        boolean showTajweed = store.getBoolean(Consts.TAJWEED_KEY, true);
        if (showArabic && showTajweed) {
            ghunna = Pattern.compile("[\u0646\u0645]\u0651.\u0670?").matcher("");
            iqlab = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u06e2\u06ed].?.? *\u0628\u0651?.\u0670?\u0653?").matcher("");
            qalqala = Pattern.compile("[\u0642\u0637\u0628\u062c\u062f](\u0652|[^اىو]{0,2}$)").matcher("");
            idgham = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u0652\u06e1\u0627\u0649\u06db\u06da\u06d7\u06d6\u06d9]* +" + "[\u0646\u0645\u064a\u0648]\u0651?.\u0670?\u0653?|\u0645[\u06db\u06da\u06d7\u06d6\u06d9\u0652\u06e1]* +\u0645\u0651?.\u0670?\u0653?").matcher("");
            ikhfa = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u06e1\u0627\u0649\u06db\u06da\u06d7\u06d6\u06d9 ]*" + "[\u0635\u0630\u062b\u0643\u062c\u0634\u0642\u0633\u062f\u0637\u0632\u0641\u062a\u0636\u0638\u06a9]\u0651?.\u0670?\u0653?" + "|\u0645[\u0652\u06e1 ]*\u0628\u0651?.\u0670?\u0653?").matcher("");
        }

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        RecyclerView recyclerView = drawerLayout.findViewById(R.id.main_list);
        viewPager2 = drawerLayout.findViewById(R.id.view_pager2);
        viewPager2.setAdapter(null);
        if (fullQuranView) {
            drawerLayout.removeView(viewPager2);
            viewPager2 = null;
            LinearLayoutManager layoutManager = new MyLayoutManager(this);
            fullQuranAdapter = new FullQuranAdapter(this, layoutManager);
            recyclerView.setAdapter(fullQuranAdapter);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.addOnScrollListener(new ChangeTitleOnScrollListener());
        } else {
            drawerLayout.removeView(recyclerView);
            viewPager2.setAdapter(new SuraPageAdapter(getSupportFragmentManager(), getLifecycle(), rtl));
            viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    currentSuraIndex = sura2position(position);
                    changeTitle();
                }
            });
        }
        Toolbar toolbar = drawerLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        drawerList.setAdapter(new QuranDrawerAdapter(this));
        drawerList.setLayoutManager(new MyLayoutManager(this));
        drawerList.addOnScrollListener(new DrawerScrollListener());
        ActionBarDrawerToggle drawerToggle = new MyDrawerToggle(this, drawerLayout, toolbar);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        drawerLayout.post(this::scrollToLastPosition);
        //initController
        playerController.findViewById(R.id.prev).setOnClickListener(this);
        playerController.findViewById(R.id.play).setOnClickListener(this);
        playerController.findViewById(R.id.next).setOnClickListener(this);
        SeekBar seekBar = playerController.findViewById(R.id.speed_seek);
        TextView textView = playerController.findViewById(R.id.player_speed);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            seekBar.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        } else {
            seekBar.setTag(textView);
            seekBar.setOnSeekBarChangeListener(this);
            int speed = store.getInt(Consts.PLAYER_SPEED, Consts.DEF_PLAYER_SPEED) - Consts.MIN_PLAYER_SPEED;
            if (speed == 0) onProgressChanged(seekBar, 0, false);
            else seekBar.setProgress(speed);
        }
        playerController.findViewById(R.id.close_scroll).setOnClickListener(this);
        playerController.findViewById(R.id.speed_down).setOnClickListener(this);
        playerController.findViewById(R.id.speed_up).setOnClickListener(this);
        playerController.findViewById(R.id.pause_scroll).setOnClickListener(this);
        playerController.findViewById(R.id.scroll_button).setOnClickListener(this);
        TextView showRepeat = playerController.findViewById(R.id.repeat);
        String s;
        int count = store.getInt(Consts.REPEAT, 1);
        if (count == Integer.MAX_VALUE) s = getString(R.string.infinity);
        else s = String.valueOf(count);
        showRepeat.setText(s);
        showRepeat.setOnClickListener(this);
        playerController.findViewById(R.id.expand).setOnClickListener(this);
        CompoundButton autoScrollSW = playerController.findViewById(R.id.auto_scroll);
        CompoundButton suraEnd = playerController.findViewById(R.id.sura_end);
        EditText stopTime = playerController.findViewById(R.id.stop_time);
        stopTime.setOnEditorActionListener(this);
        EditText delayTime = playerController.findViewById(R.id.delay_time);
        delayTime.setOnEditorActionListener(this);
        autoScrollSW.setChecked(scrollWithPlayer);
        suraEnd.setOnCheckedChangeListener(this);
        autoScrollSW.setOnCheckedChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.printer, menu);
        inflater.inflate(R.menu.search, menu);
        inflater.inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.search) startActivity(new Intent(this, SearchQuranActivity.class));
        else if (id == R.id.setting) startActivity(new Intent(this, SettingActivity.class));
        else if (id == R.id.print) startActivity(new Intent(this, PrintActivity.class));
        return true;
    }

    void startScroll() {
        AutoScroller scroller = new AutoScroller(this, scrollTime);
        if (fullQuranView) {
            scroller.setTargetPosition(fullQuranAdapter.getItemCount() - 1);
            fullQuranAdapter.layoutManager.startSmoothScroll(scroller);
        } else {
            SuraViewAdapter adapter = getAdapter(currentSuraIndex);
            if (adapter == null) return;
            scroller.setTargetPosition(adapter.getItemCount() - 1);
            adapter.layoutManager.startSmoothScroll(scroller);
        }
        if (!isAutoScrolling) {
            ImageView imageView = playerController.findViewById(R.id.pause_scroll);
            if (imageView != null) imageView.setImageResource(R.drawable.ic_pause);
            isAutoScrolling = true;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else {
            saveRecentPos();
            super.onBackPressed();
        }
    }


    private void saveRecentPos() {
        LinearLayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) return;
        int firstVisiblePos = layoutManager.findFirstVisibleItemPosition();
        if (firstVisiblePos == RecyclerView.NO_POSITION) return;
        if (firstVisiblePos < layoutManager.findLastVisibleItemPosition() - 1) {
            firstVisiblePos++;
        }
        new UpdateRecentTask(currentSuraIndex, fullQuranView ? firstVisiblePos - allSuraInfo.get(currentSuraIndex).suraStartIndex : firstVisiblePos).start();
    }

    @Nullable
    private LinearLayoutManager getLayoutManager() {
        if (fullQuranView) {
            FullQuranAdapter adapter = getAdapter();
            if (adapter != null) return adapter.layoutManager;
        } else {
            SuraViewAdapter adapter = getAdapter(currentSuraIndex);
            if (adapter != null) return adapter.layoutManager;
        }
        return null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService();
        playerListener = null;
        playingPosition = RecyclerView.NO_POSITION;
        SharedPreferences.Editor editor = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit();
        editor.putInt(Consts.RECENT_SURA_KEY, currentSuraIndex);
        LinearLayoutManager layoutManager = getLayoutManager();
        if (layoutManager != null) {
            int position = layoutManager.findFirstVisibleItemPosition();
            editor.putInt(Consts.RECENT_AYAH_KEY, fullQuranView ? position - allSuraInfo.get(currentSuraIndex).suraStartIndex : position);
            View child = layoutManager.getChildAt(0);
            editor.putInt(Consts.RECENT_OFFSET, child == null ? 0 : child.getTop());
        } else editor.putInt(Consts.RECENT_AYAH_KEY, 0);
        editor.apply();
    }

    @Override
    protected void onStart() {
        if (fullQuranView) playerListener = new FullQuranPlayerListener(this);
        else playerListener = new SuraViewPlayerListener(this);
        if (RecreateManager.needRecreate(RecreateManager.QURAN_ACTIVITY)) recreate();
        else if (QuranPlayerService.audioPlayer != null) {
            if (QuranPlayerService.audioPlayer.isPlaying()) hideScrollButton();
            else showScrollButton();
            bindService();
        }
        super.onStart();
    }


    private int sura2position(int sura) {
        if (rtl) return Consts.SURA_COUNT - 1 - sura;
        return sura;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        playerService = ((QuranPlayerService.LocalBinder) binder).getService();
        CompoundButton suraEnd = playerController.findViewById(R.id.sura_end);
        if (suraEnd != null) suraEnd.setChecked(playerService.isStopAfterSura());
        if (playingPosition == RecyclerView.NO_POSITION) try {
            playingPosition = allSuraInfo.get(playerService.playingSuraIndex()).suraStartIndex + playerService.playingAyah();
            if (fullQuranView) {
                FullQuranAdapter adapter = getAdapter();
                if (adapter != null)
                    adapter.notifyItemChanged(playingPosition, new Object[]{SuraViewAdapter.HIGHLIGHT_WHOLE_AYAH, true});
            } else if (playerService.playingSuraIndex() == currentSuraIndex) {
                SuraViewAdapter adapter = getAdapter(currentSuraIndex);
                if (adapter != null)
                    adapter.notifyItemChanged(playerService.playingAyah(), new Object[]{SuraViewAdapter.HIGHLIGHT_WHOLE_AYAH, true});
            }
            if (scrollWithPlayer && QuranPlayerService.audioPlayer.isPlaying())
                jumpTo(playerService.playingSuraIndex(), playerService.playingAyah());
        } catch (Exception e) {
            L.d(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        L.d("service disconnected");
        playerService = null;
    }

    void scrollToLastPosition() {
        Intent intent = getIntent();
        int sura = intent.getIntExtra(Consts.EXTRA_SURA_ID, -1);
        if (sura != -1) {
            jumpTo(sura, intent.getIntExtra(Consts.EXTRA_AYAH_NUM, 0));
            intent.removeExtra(Consts.EXTRA_SURA_ID);
            highlightWord = intent.getIntExtra(Consts.Extra_Word_ID, -1);
        } else {
            SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
            sura = store.getInt(Consts.RECENT_SURA_KEY, 0);
            int ayah = store.getInt(Consts.RECENT_AYAH_KEY, 0);
            int offset = store.getInt(Consts.RECENT_OFFSET, 0);
            L.d("jump to:" + sura + ", " + ayah);
            jumpTo(sura, ayah, offset);
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.scroll_button) {
            playerController.findViewById(R.id.scroller).setVisibility(View.VISIBLE);
            playerController.findViewById(R.id.player).setVisibility(View.GONE);
            startScroll();
        } else if (id == R.id.prev) {
            if (playerService != null) playerService.playPrev();
            else scrollPrev();
        } else if (id == R.id.play) {
            if (playerService != null) playerService.playPause();
            else {
                int[] position = getSuraAyah();
                if (position[1] == 0) position[1] = 1;
                ActivityCompat.startForegroundService(this, new Intent(this, QuranPlayerService.class).putExtra(Consts.EXTRA_SURA_ID, position[0] + 1).putExtra(Consts.EXTRA_AYAH_NUM, position[1]));
                bindService();
                hideScrollButton();
            }
        } else if (id == R.id.next) {
            if (playerService != null) {
                if (playerService.playingWordByWord()) playerService.setPair(true);
                playerService.playNext();
            } else scrollNext();
        } else if (id == R.id.expand) {
            View view = playerController.findViewById(R.id.extra_player);
            ValueAnimator anim;
            if (view.getHeight() == 0) {
                int remainingTime;
                int delay;
                if (playerService != null) {
                    remainingTime = playerService.remainingTime();
                    delay = playerService.verseDelay();
                } else {
                    remainingTime = 0;
                    delay = 0;
                }
                if (remainingTime != Integer.MAX_VALUE)
                    ((EditText) view.findViewById(R.id.stop_time)).setText(String.valueOf(remainingTime));
                ((EditText) view.findViewById(R.id.delay_time)).setText(String.valueOf(delay));
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate180));
                view.measure(0, 0);
                anim = ValueAnimator.ofInt(0, view.getMeasuredHeight());
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_back180));
                anim = ValueAnimator.ofInt(view.getMeasuredHeight(), 0);
            }
            anim.addUpdateListener(valueAnimator -> {
                int val = (Integer) valueAnimator.getAnimatedValue();
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = val;
                view.setLayoutParams(layoutParams);
            });
            anim.setDuration(300);
            anim.start();
        } else if (id == R.id.repeat) {
            EditText editText = (EditText) getLayoutInflater().inflate(R.layout.enter_repeat, null);
            editText.setOnEditorActionListener(this);
            alertDialog = new AlertDialog.Builder(this).setTitle(R.string.repeat_hint).setView(editText).setPositiveButton(R.string.ok, this).setNeutralButton(R.string.cancel, null).show();
        } else if (id == R.id.close_scroll) {
            stopScroll();
            playerController.findViewById(R.id.scroller).setVisibility(View.GONE);
            playerController.findViewById(R.id.player).setVisibility(View.VISIBLE);
        } else if (id == R.id.pause_scroll) {
            if (isAutoScrolling) {
                stopScroll();
            } else {
                startScroll();
            }
        } else if (id == R.id.speed_down && scrollTime < Consts.MAX_SCROLL_TIME) {
            scrollTime *= Consts.SCROLL_TIME_MULTIPLIER;
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putFloat(Consts.SCROLL_SPEED, scrollTime).apply();
            startScroll();
        } else if (id == R.id.speed_up && scrollTime > Consts.MIN_SCROLL_TIME) {
            scrollTime /= Consts.SCROLL_TIME_MULTIPLIER;
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putFloat(Consts.SCROLL_SPEED, scrollTime).apply();
            startScroll();
        }
    }

    private int[] getSuraAyah() {
        if (fullQuranView) {
            for (int i = 0; i < fullQuranAdapter.getItemCount(); i++) {
                View view = fullQuranAdapter.layoutManager.getChildAt(i);
                if (view == null) continue;
                AyahItem item = (AyahItem) view.getTag();
                if (item != null) return new int[]{item.suraIndex, item.ayah};
            }
            return new int[]{0, 0};
        }
        SuraViewAdapter adapter = getAdapter(currentSuraIndex);
        if (adapter == null) return new int[]{currentSuraIndex, 0};
        return new int[]{currentSuraIndex, adapter.layoutManager.findFirstVisibleItemPosition()};
    }

    private void scrollNext() {
        if (fullQuranView) {
            int p = fullQuranAdapter.layoutManager.findFirstVisibleItemPosition() + 1;
            if (p >= fullQuranAdapter.getItemCount()) return;
            fullQuranAdapter.layoutManager.scrollToPositionWithOffset(p, 0);
        } else {
            SuraViewAdapter adapter = getAdapter(currentSuraIndex);
            if (adapter == null) return;
            int p = adapter.layoutManager.findFirstVisibleItemPosition() + 1;
            if (p >= adapter.getItemCount()) return;
            adapter.layoutManager.scrollToPositionWithOffset(p, 0);
        }
    }

    private void scrollPrev() {
        if (fullQuranView) {
            View child = fullQuranAdapter.layoutManager.getChildAt(0);
            int p = fullQuranAdapter.layoutManager.findFirstVisibleItemPosition();
            if (child != null && child.getTop() == 0) p--;
            if (p < 0) return;
            fullQuranAdapter.layoutManager.scrollToPositionWithOffset(p, 0);
        } else {
            SuraViewAdapter adapter = getAdapter(currentSuraIndex);
            if (adapter == null) return;
            View child = adapter.layoutManager.getChildAt(0);
            int p = adapter.layoutManager.findFirstVisibleItemPosition();
            if (child != null && child.getTop() == 0) p--;
            if (p < 0) return;
            adapter.layoutManager.scrollToPositionWithOffset(p, 0);
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        String s = v.getText().toString().trim();
        if (!Pattern.compile("\\d+").matcher(s).matches()) return true;
        if (v.getId() == R.id.repeat) {
            alertDialog.dismiss();
            setRepeat(s);
        } else if (v.getId() == R.id.delay_time) {
            if (playerService != null) playerService.setDelay(Integer.parseInt(s));
        } else {
            if (playerService != null) playerService.startTimer(Integer.parseInt(s));
            v.clearFocus();
        }
        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Window window = alertDialog.getWindow();
        if (window == null) return;
        String text = ((TextView) (window.findViewById(R.id.repeat))).getText().toString().trim();
        if (Pattern.compile("\\d+").matcher(text).matches()) setRepeat(text);
    }

    private void setRepeat(String s) {
        int count = Integer.parseInt(s);
        if (count == 0) {
            count = Integer.MAX_VALUE;
            s = getString(R.string.infinity);
        }
        getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit().putInt(Consts.REPEAT, count).apply();
        playerService.setRepeat(count);
        TextView textView = playerController.findViewById(R.id.repeat);
        if (textView != null) textView.setText(s);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit();
        if (buttonView.getId() == R.id.auto_scroll) {
            scrollWithPlayer = isChecked;
            editor.putBoolean(Consts.AUTO_SCROLL, isChecked);
        } else {
            if (playerService != null) playerService.stopAfterSura(isChecked);
            editor.putBoolean(Consts.STOP_SURA, isChecked);
        }
        editor.apply();
    }

    void jumpTo(int sura, int ayah, int offset) {
        if (fullQuranView) {
            getAdapter().layoutManager.scrollToPositionWithOffset(allSuraInfo.get(sura).suraStartIndex + ayah, offset);
        } else {
            if (sura == currentSuraIndex) {
                SuraViewAdapter adapter = getAdapter(sura);
                if (adapter != null) {
                    adapter.layoutManager.scrollToPositionWithOffset(ayah, offset);
                }
            } else {
                viewPager2.setCurrentItem(sura2position(sura), false);
                if (ayah != 0 || offset != 0)
                    viewPager2.post(() -> jumpTo(currentSuraIndex, ayah, offset));
            }
        }
        currentSuraIndex = sura;
        changeTitle();
    }

    @Nullable SuraViewAdapter getAdapter(int suraIndex) {
        FragmentManager fm = getSupportFragmentManager();
        for (Fragment fragment : fm.getFragments()) {
            SuraViewFragment suraViewFragment = (SuraViewFragment) fragment;
            if (suraViewFragment.suraIndex == suraIndex) return suraViewFragment.adapter;
        }
        return null;
    }

    boolean withoutArabic() {
        return viewPool == null;
    }

    FullQuranAdapter getAdapter() {
        return fullQuranAdapter;
    }

    void changeTitle() {
        setTitle(Utils.formatNum(currentSuraIndex + 1) + ". " + allSuraInfo.get(currentSuraIndex).name);
    }

    void jumpTo(int sura, int ayah) {
        jumpTo(sura, ayah, 0);
    }

    void unbindService() {
        if (playerService != null) {
            unbindService(this);
            playerService = null;
        }
    }

    void stopScroll() {
        isAutoScrolling = false;
        long uptime = SystemClock.uptimeMillis();
        findViewById(R.id.main_list).dispatchTouchEvent(MotionEvent.obtain(uptime, uptime + 100, MotionEvent.ACTION_UP, 0, 0, 0));
        ImageView imageView = playerController.findViewById(R.id.pause_scroll);
        if (imageView != null) imageView.setImageResource(R.drawable.ic_play);
    }

    boolean justifyTrans() {
        return justifyTrans;
    }

    boolean scrollWithPlayer() {
        return scrollWithPlayer && !isAutoScrolling;
    }

    public boolean isUserDragging() {
        return userIsDragging;
    }

    public void changeDraggingState(boolean dragging) {
        userIsDragging = dragging;
    }

    public void hideScrollButton() {
        L.d("hiding scroll button");
        playerController.findViewById(R.id.repeat).setVisibility(View.VISIBLE);
        playerController.findViewById(R.id.scroll_button).setVisibility(View.GONE);
        ((ImageView) playerController.findViewById(R.id.play)).setImageResource(R.drawable.ic_pause);

    }

    public void showScrollButton() {
        L.d("showing scroll button");
        playerController.findViewById(R.id.repeat).setVisibility(View.GONE);
        playerController.findViewById(R.id.scroll_button).setVisibility(View.VISIBLE);
        ((ImageView) playerController.findViewById(R.id.play)).setImageResource(R.drawable.ic_play);

    }


    void bindService() {
        if (playerService == null)
            bindService(new Intent(this, QuranPlayerService.class), this, BIND_ABOVE_CLIENT);
    }

    public boolean hasWordMeaning() {
        return wordDBIds != null;
    }

    public boolean hasTranslation() {
        return translationNames != null;
    }

    public int getPosition(AyahItem item) {
        return allSuraInfo.get(item.suraIndex).suraStartIndex + item.ayah;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        progress += Consts.MIN_PLAYER_SPEED;
        float speed = progress / 10.0f;
        if (playerService != null) playerService.setPlayerSpeed(speed);
        TextView textView = (TextView) seekBar.getTag();
        textView.setText(getString(R.string.player_speed, speed));
        getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(Consts.PLAYER_SPEED, progress).apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public int playingSuraIndex() {
        if (playerService != null) return playerService.playingSuraIndex();
        return currentSuraIndex;
    }

    static class SuraInfo {
        final int suraStartIndex;
        final int totalAyah;
        final boolean makki;
        final String name;

        SuraInfo(int rowId, int totalAyah, int word, String name) {
            this.suraStartIndex = rowId - 1;
            this.totalAyah = totalAyah;
            this.makki = word == 0;
            this.name = name;
        }
    }
}
