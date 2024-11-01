package bn.poro.quran.fragments.trans_load;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class MySpinnerAdapter extends BaseAdapter {
    private final String[] strings;

    MySpinnerAdapter() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("SELECT lang from trans group by lang order by count(lang) desc", null);
        strings = new String[cursor.getCount()];
        for (int i = 0; i < strings.length; i++) {
            cursor.moveToPosition(i);
            strings[i] = cursor.getString(0);
        }
        cursor.close();
        database.close();
    }

    @Override
    public int getCount() {
        return strings.length + 1;
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
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dialog, parent, false);
        if (position == 0) ((TextView) convertView).setText(R.string.all_lang);
        else {
            convertView.setTag(strings[position - 1]);
            ((TextView) convertView).setText(Utils.getLanguageName(strings[position - 1]));
        }
        return convertView;
    }
}
