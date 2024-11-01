package bn.poro.quran.fragments.setting_main;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;

public class LangAdapter extends BaseAdapter implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private final String[] languageCodes;
    private final Activity activity;
    private final String selectedLang;

    public LangAdapter(Activity activity) {
        this.activity = activity;
        languageCodes = activity.getResources().getStringArray(R.array.language_codes);
        selectedLang = MainActivity.getAppLang();
    }

    public static void setLang(String lang_code) {
        if (lang_code.isEmpty())
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        else
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang_code));
    }

    @Override
    public int getCount() {
        return languageCodes.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = activity.getLayoutInflater()
                    .inflate(R.layout.radio_text, parent, false);
        } else if (position == (int) convertView.getTag()) return convertView;
        convertView.setTag(position);
        CompoundButton button = (CompoundButton) convertView;
        String lang = languageCodes[position];
        boolean selected = lang.equalsIgnoreCase(selectedLang);
        button.setText(Utils.getLanguageName(lang));
        button.setChecked(selected);
        return button;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (!languageCodes[i].equals(selectedLang)) {
            setLang(languageCodes[i]);
        } else if (activity instanceof MainActivity) ((MainActivity) activity).onLangReady();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        ((MainActivity) activity).onLangReady();
    }
}
