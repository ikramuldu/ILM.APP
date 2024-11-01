package bn.poro.quran.fragments.setting_main;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.fragments.audio_load.AudioLoadFragment;
import bn.poro.quran.fragments.trans_load.TransLoadFragment;
import bn.poro.quran.fragments.word_load.WordLoadFragment;
import bn.poro.quran.views.FontSpan;

public class SettingFragment extends Fragment implements
        SeekBar.OnSeekBarChangeListener,
        View.OnClickListener,
        DialogInterface.OnClickListener,
        CopyTask.CopyListener {
    private String[] drives;
    SettingActivity activity;
    AlertDialog dialog;
    int arabicFontIndex, arabicFontSize;
    private View scrollView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (SettingActivity) inflater.getContext();
        final SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        int themeID = store.getInt(Consts.THEME_KEY, 0);
        boolean ar = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        boolean fullQuranView = store.getBoolean(Consts.FULL_QURAN_VIEW, false);
        int fontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC);
        arabicFontIndex = store.getInt(Consts.ARABIC_FONT_FACE, 1);
        scrollView = inflater.inflate(R.layout.fragment_setting, container, false);
        CardView langButton = scrollView.findViewById(R.id.language);
        ((TextView) langButton.getChildAt(0)).setText(activity.createSpan(getString(R.string.pref_settings_choose_language), getAppLanguage()));
        langButton.setOnClickListener(this);
        activity.setSupportActionBar(scrollView.findViewById(R.id.toolbar));
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        activity.setTitle(R.string.menu_settings);
        CardView ThemeSelect = scrollView.findViewById(R.id.theme_switch);
        String themeName = getResources().getStringArray(R.array.theme_names)[themeID];
        ((TextView) ThemeSelect.getChildAt(0)).setText(activity.createSpan(getString(R.string.pref_theme), themeName));
        CardView cardView = scrollView.findViewById(R.id.arabic_font);
        cardView.setOnClickListener(this);
        CompoundButton arabicSW = scrollView.findViewById(R.id.arabic_switch);
        CompoundButton fullQuranMode = scrollView.findViewById(R.id.view_switch);
        CompoundButton directionSW = scrollView.findViewById(R.id.direction_switch);
        CompoundButton showByWord = scrollView.findViewById(R.id.by_word);
        CompoundButton showTrans = scrollView.findViewById(R.id.trans_check);
        CompoundButton justification = scrollView.findViewById(R.id.justify);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            justification.setChecked(store.getBoolean(Consts.JUSTIFICATION, false));
            justification.setOnClickListener(this);
        } else justification.setVisibility(View.GONE);
        CompoundButton tajweedSwitch = scrollView.findViewById(R.id.tajweed_switch);
        arabicSW.setChecked(ar);
        arabicSW.setTag(tajweedSwitch);
        showByWord.setChecked(store.getBoolean(Consts.SHOW_BY_WORD, true));
        showTrans.setChecked(store.getBoolean(Consts.SHOW_TRANS, true));
        if (ar) tajweedSwitch.setChecked(store.getBoolean(Consts.TAJWEED_KEY, true));
        else {
            tajweedSwitch.setChecked(false);
            tajweedSwitch.setEnabled(false);
        }
        fullQuranMode.setChecked(fullQuranView);
        fullQuranMode.setTag(directionSW);
        directionSW.setChecked(store.getBoolean(Consts.SURA_R2L, Consts.DEFAULT_DIRECTION_R2L));
        directionSW.setEnabled(!fullQuranView);
        tajweedSwitch.setOnClickListener(this);
        scrollView.findViewById(R.id.word).setOnClickListener(this);
        ThemeSelect.setOnClickListener(this);
        scrollView.findViewById(R.id.trans_switch).setOnClickListener(this);
        arabicSW.setOnClickListener(this);
        fullQuranMode.setOnClickListener(this);
        directionSW.setOnClickListener(this);
        showTrans.setOnClickListener(this);
        showByWord.setOnClickListener(this);
        scrollView.findViewById(R.id.audio_manager).setOnClickListener(this);
        SeekBar seekFont = scrollView.findViewById(R.id.font_seek);
        seekFont.setTag(scrollView.findViewById(R.id.font));
        seekFont.setOnSeekBarChangeListener(this);
        if (fontSize == Consts.MIN_FONT) onProgressChanged(seekFont, 0, false);
        else seekFont.setProgress(fontSize - Consts.MIN_FONT);

        SeekBar arabicSizeSeek = scrollView.findViewById(R.id.arabic_seek);
        arabicSizeSeek.setTag(scrollView.findViewById(R.id.arabic));
        arabicSizeSeek.setOnSeekBarChangeListener(this);
        if (arabicFontSize == Consts.MIN_FONT_ARABIC) onProgressChanged(arabicSizeSeek, 0, false);
        else arabicSizeSeek.setProgress(arabicFontSize - Consts.MIN_FONT_ARABIC);

        HashSet<String> pathSet = new HashSet<>();
        File[] files = activity.getExternalFilesDirs(null);
        for (File file : files)
            if (file != null) {
                if (!file.exists() && !file.mkdirs()) continue;
                pathSet.add(file.getPath() + File.separator);
            }
        CardView storageCard = scrollView.findViewById(R.id.storage_card);
        if (pathSet.size() == 1) storageCard.setVisibility(View.GONE);
        else {
            drives = new String[pathSet.size()];
            Iterator<String> iterator = pathSet.iterator();
            for (int i = 0; i < drives.length; i++) drives[i] = iterator.next();
            ((TextView) storageCard.getChildAt(0)).setText(activity.createSpan(getString(R.string.prefs_app_location_title), Utils.dataPath));
            storageCard.setOnClickListener(this);
        }
        return scrollView;
    }

    private String getAppLanguage() {
        Locale appLocale = MainActivity.getLocale();
        String systemLang;
        try {
            String s = Settings.System.getString(activity.getContentResolver(), "system_locales");
            systemLang = s.split(",")[0].split("-")[0];
        } catch (Exception e) {
            systemLang = "en";
        }
        String s1 = appLocale.getDisplayLanguage(appLocale);
        String s2 = appLocale.getDisplayLanguage(new Locale(systemLang));
        return s1.equals(s2) ? s1 : s1 + " - " + s2;
    }

    @Override
    public void onStart() {
        super.onStart();
        SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        sqLiteDatabase.execSQL("ATTACH DATABASE ? AS sts", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
        Cursor cursor = sqLiteDatabase.rawQuery("select group_concat(name,', ') from trans inner join sts.status using (id) where sts.status.extra=1", null);
        String text = null;
        if (cursor.moveToNext()) text = cursor.getString(0);
        if (text == null) text = getString(R.string.not_set);
        ((TextView) scrollView.findViewById(R.id.trans_switch)).setText(activity.createSpan(activity.getString(R.string.translation_and_tafsir), text));
        cursor.close();
        cursor = sqLiteDatabase.rawQuery("select name from word inner join sts.status using (id) where sts.status.extra=1", null);
        StringBuilder stringBuilder = new StringBuilder();
        while (cursor.moveToNext()) {
            if (stringBuilder.length() != 0)
                stringBuilder.append(", ");
            Locale locale = new Locale(cursor.getString(0));
            stringBuilder.append(locale.getDisplayName(locale));
        }
        if (stringBuilder.length() == 0) stringBuilder.append(getString(R.string.not_set));
        ((TextView) scrollView.findViewById(R.id.word)).setText(activity.createSpan(activity.getString(R.string.word_by_word_language), stringBuilder));
        cursor.close();
        sqLiteDatabase.close();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        TextView textView = (TextView) seekBar.getTag();
        if (seekBar.getId() == R.id.arabic_seek) {
            arabicFontSize = progress + Consts.MIN_FONT_ARABIC;
            updateArabicFont();
            textView.setText(String.format("%s: %d", getString(R.string.pref_arabic_font_size), arabicFontSize));
        } else {
            progress += Consts.MIN_FONT;
            textView.setTextSize(progress);
            textView.setText(String.format("%s: %d", getString(R.string.pref_translation_font_size), progress));
        }
    }

    void updateArabicFont() {
        SpannableStringBuilder fontInfo = new SpannableStringBuilder(getString(R.string.pref_arabic_font));
        fontInfo.append("\n");
        int start = fontInfo.length();
        if (arabicFontIndex >= 5) fontInfo.append(getString(R.string.indo));
        else fontInfo.append(getString(R.string.bismillah));
        int fontFaceId = Consts.FONT_LIST[arabicFontIndex];
        int end = fontInfo.length();
        if (fontFaceId != 0) {
            fontInfo.setSpan(new FontSpan(ResourcesCompat.getFont(activity, fontFaceId)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        fontInfo.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) scrollView.findViewById(R.id.arabic_font_sample)).setText(fontInfo);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        String key;
        int value = seekBar.getProgress();
        if (seekBar.getId() == R.id.arabic_seek) {
            key = Consts.ARABIC_FONT_KEY;
            value += Consts.MIN_FONT_ARABIC;
        } else {
            key = Consts.FONT_KEY;
            value += Consts.MIN_FONT;
        }
        RecreateManager.recreateAll();
        activity.getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    @Override
    public void onClick(View v) {
        int vid = v.getId();
        if (vid == R.id.language) {
            LangAdapter adapter = new LangAdapter(activity);
            new AlertDialog.Builder(activity).setTitle(R.string.pref_settings_choose_language)
                    .setAdapter(adapter, adapter)
                    .setNeutralButton(R.string.cancel, null)
                    .show();
            return;
        }
        if (vid == R.id.arabic_font) {
            FontListAdapter adapter = new FontListAdapter(this);
            dialog = new AlertDialog.Builder(activity).setTitle(R.string.pref_arabic_font)
                    .setAdapter(adapter, null)
                    .setNeutralButton(R.string.cancel, null)
                    .show();
            return;
        }
        if (vid == R.id.theme_switch) {
            ThemeListAdapter adapter = new ThemeListAdapter(activity);
            new AlertDialog.Builder(activity).setTitle(R.string.pref_theme)
                    .setAdapter(adapter, adapter)
                    .setNeutralButton(R.string.cancel, null)
                    .show();
            return;
        }

        if (vid == R.id.audio_manager) {
            Utils.replaceFragment(activity, new AudioLoadFragment());
            return;
        }
        if (vid == R.id.storage_card) {
            new AlertDialog.Builder(activity).setTitle(R.string.prefs_app_location_title)
                    .setAdapter(new DriveAdapter(drives, activity.secondaryColor), this).show();
            return;
        }
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (vid == R.id.word) {
            Utils.replaceFragment(activity, new WordLoadFragment());
            return;
        }
        if (vid == R.id.trans_switch) {
            Utils.replaceFragment(activity, new TransLoadFragment());
            return;
        }
        if (!(v instanceof CompoundButton)) return;
        String key;
        boolean isChecked = ((CompoundButton) v).isChecked();
        if (vid == R.id.tajweed_switch) {
            key = Consts.TAJWEED_KEY;
        } else if (vid == R.id.justify) {
            key = Consts.JUSTIFICATION;
            if (!store.contains(Consts.JUSTIFICATION_NOTIFIED)) {
                new AlertDialog.Builder(activity)
                        .setMessage(R.string.justify_note)
                        .setPositiveButton(R.string.ok, null).show();
                store.edit().putString(Consts.JUSTIFICATION_NOTIFIED, "").apply();
            }
        } else if (vid == R.id.by_word) {
            key = Consts.SHOW_BY_WORD;
        } else if (vid == R.id.trans_check) {
            key = Consts.SHOW_TRANS;
        } else if (vid == R.id.view_switch) {
            key = Consts.FULL_QURAN_VIEW;
            CompoundButton direction = (CompoundButton) v.getTag();
            direction.setEnabled(!isChecked);
            direction.setChecked(store.getBoolean(Consts.SURA_R2L, true));
        } else if (vid == R.id.direction_switch) {
            key = Consts.SURA_R2L;
        } else {
            key = Consts.SHOW_ARABIC_KEY;
            CompoundButton tajweed = (CompoundButton) v.getTag();
            tajweed.setEnabled(isChecked);
            tajweed.setChecked(isChecked && store.getBoolean(Consts.TAJWEED_KEY, true));
        }
        RecreateManager.recreateAll();
        store.edit().putBoolean(key, isChecked).apply();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                new CopyTask(preferences.getString(Consts.PATH_KEY, null), Utils.dataPath, this).start();
                dialog = new AlertDialog.Builder(activity).setView(R.layout.loading).setCancelable(false).show();
                preferences.edit().putString(Consts.PATH_KEY, Utils.dataPath).apply();
                RecreateManager.recreateAll();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                Utils.dataPath = preferences.getString(Consts.PATH_KEY, Utils.dataPath);
                break;
            default:
                if (!Utils.dataPath.equals(drives[which])) {
                    Utils.dataPath = drives[which];
                    new AlertDialog.Builder(activity).setTitle(R.string.warning)
                            .setMessage(R.string.copy_warning)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, this)
                            .setNegativeButton(R.string.cancel, this)
                            .show();
                }
        }
    }

    @Override
    public void onCopyComplete(String from) {
        dialog.dismiss();
        dialog = null;
        SpannableStringBuilder spannableString = new SpannableStringBuilder(getString(R.string.prefs_app_location_title));
        spannableString.append("\n");
        int start = spannableString.length();
        spannableString.append(Utils.dataPath);
        int end = spannableString.length();
        spannableString.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(activity.secondaryColor),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        View view = getView();
        if (view == null) return;
        ((TextView) view.findViewById(R.id.storage)).setText(spannableString);
    }

}

