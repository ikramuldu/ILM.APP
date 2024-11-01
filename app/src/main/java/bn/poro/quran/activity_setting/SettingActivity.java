package bn.poro.quran.activity_setting;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.Gravity;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.transition.Slide;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.fragments.setting_main.SettingFragment;

public class SettingActivity extends AppCompatActivity {

    public static final int[] THEME = {R.style.ThemeWhite,
            R.style.ThemeBlack,
            R.style.ThemeGreen,
            R.style.ThemeBrown,
            R.style.ThemeDarkBlue};
    public static final float SECONDARY_TEXT_SIZE = 0.8f;
    public int secondaryColor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        int themeID = store.getInt(Consts.THEME_KEY, 0);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setTheme(THEME[themeID]);
        setContentView(R.layout.fragment_container);
        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.tab_text});
        secondaryColor = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else
            typedArray.recycle();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentById(R.id.container) == null) {
            Fragment fragment = new SettingFragment();
            fragment.setEnterTransition(new Slide(Gravity.END));
            fragment.setExitTransition(new Slide(Gravity.START));
            fragmentManager.beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    public SpannableStringBuilder createSpan(CharSequence primary, CharSequence secondary) {
        SpannableStringBuilder spannableString = new SpannableStringBuilder(primary);
        spannableString.append("\n");
        int start = spannableString.length();
        spannableString.append(secondary);
        int end = spannableString.length();
        spannableString.setSpan(new RelativeSizeSpan(SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return true;
    }
}
