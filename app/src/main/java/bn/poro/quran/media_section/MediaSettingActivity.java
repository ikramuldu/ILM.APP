package bn.poro.quran.media_section;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.fragments.setting_main.DriveAdapter;
import bn.poro.quran.fragments.setting_main.LangAdapter;

public class MediaSettingActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener, View.OnClickListener,
        DialogInterface.OnClickListener, CopyTask.CopyListener, DownloadWithoutProgress.DownloadListener {
    private final ArrayList<String> drives = new ArrayList<>(2);
    private AlertDialog dialog;
    private String language;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.media_setting);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        View view = findViewById(R.id.views);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        CompoundButton themeSwitch = view.findViewById(R.id.theme);
        CompoundButton fitScreen = view.findViewById(R.id.fit);
        CompoundButton keepRatio = view.findViewById(R.id.ratio);
        SeekBar pSeek = view.findViewById(R.id.p_seek);
        pSeek.setTag(view.findViewById(R.id.prev));
        themeSwitch.setChecked(true);
        fitScreen.setChecked(store.getBoolean(Consts.FIT_SCREEN_KEY, true));
        keepRatio.setChecked(store.getBoolean(Consts.KEEP_RATIO_KEY, true));
        pSeek.setOnSeekBarChangeListener(this);
        pSeek.setProgress(store.getInt(Consts.BACKWARD_KEY, 10));
        themeSwitch.setOnCheckedChangeListener(this);
        fitScreen.setOnCheckedChangeListener(this);
        keepRatio.setOnCheckedChangeListener(this);

        SeekBar seekFont = findViewById(R.id.bangla_seek);
        seekFont.setTag(findViewById(R.id.bangla));
        seekFont.setOnSeekBarChangeListener(this);
        seekFont.setProgress(store.getInt(Consts.FONT_KEY, Consts.DEF_FONT) - Consts.MIN_FONT);

        TextView storage = view.findViewById(R.id.storage);
        File[] files = ContextCompat.getExternalFilesDirs(this, null);
        for (File file : files) {
            if (file != null) drives.add(file.getPath() + File.separator);
        }
        if (drives.size() <= 1) storage.setVisibility(View.GONE);
        else {
            SpannableStringBuilder spannableString = new SpannableStringBuilder(getString(R.string.pref_audio_manager));
            int len = spannableString.length();
            spannableString.append(Utils.dataPath);
            spannableString.setSpan(new RelativeSizeSpan(0.7f), len, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            storage.setText(spannableString);
            storage.setOnClickListener(this);
        }

        TextView lang = findViewById(R.id.language);
        setLangText();
        lang.setOnClickListener(this);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String key;
        int id = buttonView.getId();
        if (id == R.id.theme) {
            key = Consts.THEME_KEY;
            recreate();
        } else if (id == R.id.fit) {
            key = Consts.FIT_SCREEN_KEY;
        } else {
            key = Consts.KEEP_RATIO_KEY;
        }
        getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putBoolean(key, isChecked).apply();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        TextView textView = (TextView) seekBar.getTag();
        if (seekBar.getId() == R.id.bangla_seek) {
            progress += Consts.MIN_FONT;
            textView.setText(String.format(new Locale(language), "Font size %d Px", progress));
            textView.setTextSize(progress);
        } else {
            textView.setText(String.format(new Locale(language), "Double tap: %d sec progress", progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String key;
        int value = seekBar.getProgress();
        if (seekBar.getId() == R.id.bangla_seek) {
            key = Consts.FONT_KEY;
            value += Consts.MIN_FONT;
        } else {
            key = Consts.BACKWARD_KEY;
        }
        getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.language) new AlertDialog.Builder(this)
                .setTitle(R.string.pref_settings_choose_language)
                .setAdapter(new LangAdapter(this), (d, which) -> {
                    language = MainActivity.getAppLang();
                    if (!new File(Utils.dataPath + language + ".db").exists()) {
                        dialog = new AlertDialog.Builder(this).setView(R.layout.loading).setCancelable(false).show();
                        new DownloadWithoutProgress(new File(Utils.dataPath + language + ".db"), DownloadService.BASE_URL + "lang/" + language + ".zip", this).start();
                    } else {
                        getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE)
                                .edit().putString(Consts.LANGUAGE_CODE, language).apply();
                        recreate();
                    }
                }).show();
        else new AlertDialog.Builder(this).setTitle(R.string.prefs_app_location_title)
                .setAdapter(new DriveAdapter((String[]) drives.toArray(), Color.BLUE), this).show();
    }

    private void setLangText() {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder("Change Language\n");
        stringBuilder.setSpan(new ForegroundColorSpan(0xbb25b7d3), stringBuilder.length(),
                stringBuilder.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        stringBuilder.append(language);
        ((TextView) findViewById(R.id.language)).setText(stringBuilder);
    }


    @Override
    public void onDownloaded(File file, int code) {
        dialog.dismiss();
        if (code == DownloadRunnable.COMPLETED) {
            getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit().putString(Consts.LANGUAGE_CODE, language).apply();
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + language + ".db",
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("alter table data add column size INTEGER;");
            database.close();
            recreate();
        } else Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(DialogInterface d, int which) {
        SharedPreferences preferences = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            new CopyTask(preferences.getString(Consts.PATH_KEY, null), Utils.dataPath, this).start();
            dialog = new AlertDialog.Builder(this).setView(R.layout.loading).setCancelable(false).show();
            preferences.edit().putString(Consts.PATH_KEY, Utils.dataPath).apply();
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Utils.dataPath = preferences.getString(Consts.PATH_KEY, Utils.dataPath);
        } else if (!Utils.dataPath.equals(drives.get(which))) {
            Utils.dataPath = drives.get(which);
            new AlertDialog.Builder(this).setTitle(R.string.warning)
                    .setMessage(R.string.copy_warning)
                    .setCancelable(false)
                    .setPositiveButton(R.string.proceed, this)
                    .setNegativeButton(R.string.cancel, this)
                    .show();
        }
    }

    @Override
    public void onCopyComplete(String from) {
        dialog.dismiss();
        SpannableStringBuilder spannableString = new SpannableStringBuilder(getString(R.string.pref_audio_manager));
        int len = spannableString.length();
        spannableString.append(Utils.dataPath);
        spannableString.setSpan(new RelativeSizeSpan(0.7f), len, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(R.id.storage)).setText(spannableString);
    }
}
