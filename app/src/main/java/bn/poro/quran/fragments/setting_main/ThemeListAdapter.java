package bn.poro.quran.fragments.setting_main;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import bn.poro.quran.RecreateManager;
import bn.poro.quran.views.MyProgressBar;
import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

 class ThemeListAdapter extends BaseAdapter implements DialogInterface.OnClickListener {
    final String[] titles;
    final SettingActivity activity;
    private final int selected;

     ThemeListAdapter(SettingActivity activity) {
        this.activity = activity;
        titles = activity.getResources().getStringArray(R.array.theme_names);
        selected = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE)
                .getInt(Consts.THEME_KEY, 0);
    }

    @Override
    public int getCount() {
        return titles.length;
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
            convertView = LayoutInflater.from(activity).inflate(R.layout.radio_text, parent, false);
        } else if (position == (int) convertView.getTag()) return convertView;
        convertView.setTag(position);
        CompoundButton button = (CompoundButton) convertView;
        button.setText(titles[position]);
        button.setChecked(position == selected);
        return button;
    }


    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i != selected) {
            activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(Consts.THEME_KEY, i).apply();
            RecreateManager.recreateAll();
            MyProgressBar.paint = null;
            activity.recreate();
        }
    }
}
