package bn.poro.quran.book_section;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import bn.poro.quran.R;

class MenuAdapter extends BaseAdapter {
    final String[] titles;

    public MenuAdapter(Activity activity) {
        titles = activity.getResources().getStringArray(R.array.book_popup);
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
        if (convertView == null)
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_link, parent, false);
        TextView textView = (TextView) convertView;
        textView.setText(titles[position]);
        return convertView;
    }
}
