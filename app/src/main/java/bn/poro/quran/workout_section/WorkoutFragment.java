package bn.poro.quran.workout_section;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import bn.poro.quran.Consts;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_setting.SettingActivity;

public class WorkoutFragment extends Fragment implements
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, DrawerLayout.DrawerListener {
    static final int[] gifs = {
            R.drawable.g1,
            R.drawable.g2,
            R.drawable.g3,
            R.drawable.g5,
            R.drawable.g6,
            R.drawable.g7,
            R.drawable.g9,
            R.drawable.g10,
            R.drawable.g11,
            R.drawable.g13,
            R.drawable.g14,
            R.drawable.g15,
            R.drawable.g17,
            R.drawable.g18,
            R.drawable.g19
    };

    private final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            View view = getView();
            if (view != null) {
                DrawerLayout drawerLayout = view.findViewById(R.id.drawer_layout);
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        }
    };
    public static final int DEF_WORK = 30;
    public static final int MIN_WORK = 30;
    public static final int DEF_REST = 10;
    public static final int MIN_REST = 10;
    static final int[] workoutDetails = {
            R.string.a1
            , R.string.a2
            , R.string.a3
            , R.string.a5
            , R.string.a6
            , R.string.a7
            , R.string.a9
            , R.string.a10
            , R.string.a11
            , R.string.a13
            , R.string.a14
            , R.string.a15
            , R.string.a17
            , R.string.a18
            , R.string.a19
    };
    static final String BUTTON_ID = "d";
    static final int STARRED_ITEMS = 0b001010100111101;
    int checkedItems;
    String[] title;
    private AlertDialog dialog;
    MainActivity activity;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        activity = (MainActivity) inflater.getContext();
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        DrawerLayout layout = (DrawerLayout) inflater.inflate(R.layout.workout_main, container, false);
        layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        layout.addDrawerListener(this);
        RecyclerView recyclerView = layout.findViewById(R.id.main_list);
        activity.setSupportActionBar(layout.findViewById(R.id.toolbar));
        activity.setTitle(R.string.workout_title);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        recyclerView.setAdapter(new TitleAdapter(this));
        View button = layout.findViewById(R.id.button);
        button.setOnClickListener(this);
        ActionBar actionBar = activity.getSupportActionBar();
        activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
        checkedItems = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE)
                .getInt(Consts.CHECKED_ITEMS, STARRED_ITEMS);
        title = getResources().getStringArray(R.array.title);
        layout.findViewById(R.id.setting).setOnClickListener(this);
        SeekBar workSeek = layout.findViewById(R.id.work_time_seek);
        workSeek.setTag(layout.findViewById(R.id.work_time));
        workSeek.setProgress(1);
        workSeek.setOnSeekBarChangeListener(this);
        workSeek.setProgress(store.getInt(Consts.WORK_KEY, WorkoutFragment.DEF_WORK) - WorkoutFragment.MIN_WORK);

        SeekBar restSeek = layout.findViewById(R.id.rest_time_seek);
        restSeek.setTag(layout.findViewById(R.id.rest_time));
        restSeek.setProgress(1);
        restSeek.setOnSeekBarChangeListener(this);
        restSeek.setProgress(store.getInt(Consts.REST_KEY, WorkoutFragment.DEF_REST) - WorkoutFragment.MIN_REST);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        return layout;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.workout_options) {
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.openDrawer(GravityCompat.END);
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.workout_setting, menu);
    }

    @Override
    public void onStop() {
        super.onStop();
        activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit().putInt(Consts.CHECKED_ITEMS, checkedItems).apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView textView = (TextView) seekBar.getTag();
        if (seekBar.getId() == R.id.work_time_seek) {
            progress += WorkoutFragment.MIN_WORK;
            textView.setText(getString(R.string.work_time, progress));
        } else {
            progress += WorkoutFragment.MIN_REST;
            textView.setText(getString(R.string.rest_time, progress));
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String key;
        int value = seekBar.getProgress();
        if (seekBar.getId() == R.id.work_time_seek) {
            key = Consts.WORK_KEY;
            value += WorkoutFragment.MIN_WORK;
        } else {
            key = Consts.REST_KEY;
            value += WorkoutFragment.MIN_REST;
        }
        activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setting) {
            startActivity(new Intent(activity, SettingActivity.class));
        } else if (v.getId() == R.id.button) {
            if (dialog != null) {
                dialog.show();
                return;
            }
            View view = LayoutInflater.from(activity).inflate(R.layout.select_dialog, null);
            RecyclerView recyclerView = view.findViewById(R.id.list);
            view.findViewById(R.id.serially).setOnClickListener(this);
            view.findViewById(R.id.randomly).setOnClickListener(this);
            recyclerView.setLayoutManager(new MyLayoutManager(activity));
            recyclerView.setAdapter(new WorkoutSelectionListAdapter(this));
            dialog = new MaterialAlertDialogBuilder(activity).setView(view).show();
            dialog.show();
        } else {
            dialog.dismiss();
            startActivity(new Intent(activity, WorkoutActivity.class).putExtra(Consts.CHECKED_ITEMS, checkedItems).putExtra(BUTTON_ID, v.getId()));
        }
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        callback.setEnabled(true);
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        callback.setEnabled(false);
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
