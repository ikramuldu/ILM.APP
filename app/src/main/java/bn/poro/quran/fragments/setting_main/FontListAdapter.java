package bn.poro.quran.fragments.setting_main;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import bn.poro.quran.Consts;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.R;

class FontListAdapter extends BaseAdapter implements View.OnClickListener {
    private final SettingFragment fragment;

    FontListAdapter(SettingFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public int getCount() {
        return Consts.FONT_LIST.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null)
            view = LayoutInflater.from(fragment.activity).inflate(R.layout.item_card, viewGroup, false);
        View cardView = view.findViewById(R.id.card);
        TextView textView = cardView.findViewById(R.id.text);
        cardView.setTag(i);
        cardView.setOnClickListener(this);
        textView.setTextSize(fragment.arabicFontSize);
        if (i >= 5) textView.setText(R.string.indo);
        else textView.setText(R.string.bismillah);
        if (i == 0) textView.setTypeface(null);
        else textView.setTypeface(ResourcesCompat.getFont(fragment.activity, Consts.FONT_LIST[i]));
        return view;
    }

    @Override
    public void onClick(View v) {
        int index = (int) v.getTag();
        SharedPreferences preferences = fragment.activity.getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (preferences.getInt(Consts.ARABIC_FONT_FACE, 1) != index) {
            preferences.edit().putInt(Consts.ARABIC_FONT_FACE, index).apply();
            RecreateManager.recreateAll();
            fragment.arabicFontIndex = index;
            fragment.updateArabicFont();
        }
        fragment.dialog.dismiss();
    }
}
