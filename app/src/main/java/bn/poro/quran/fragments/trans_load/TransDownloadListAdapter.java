package bn.poro.quran.fragments.trans_load;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.AdapterView;

import java.util.Locale;

import bn.poro.quran.Consts;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;
import bn.poro.quran.fragments.word_load.WordLangDownloadListAdapter;

public class TransDownloadListAdapter extends WordLangDownloadListAdapter implements AdapterView.OnItemSelectedListener {

    public TransDownloadListAdapter(SettingActivity activity) {
        this.activity = activity;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("ATTACH DATABASE ? AS sts", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
        Cursor cursor;
        if (position == 0)
            cursor = database.rawQuery("SELECT trans.id,name,version,ver,extra,size,lang,writer from trans left outer join sts.status using (id) order by ver desc,lang='bn' desc,lang='en' desc,lang", null);
        else
            cursor = database.rawQuery("SELECT trans.id,name,version,ver,extra,size,lang,writer from trans left outer join sts.status using (id) where lang=? order by ver desc", new String[]{(String) view.getTag()});
        itemModels = new ItemModel[cursor.getCount()];
        for (int i = 0; i < itemModels.length; i++) {
            cursor.moveToPosition(i);
            Locale locale = new Locale(cursor.getString(6));
            String name = locale.getDisplayName(locale) + ": " + cursor.getString(1);
            String writer = cursor.getString(7);
            itemModels[i] = new ItemModel(cursor.getInt(0), writer == null ? name : name + ", " + writer, cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5));
        }
        cursor.close();
        database.close();
        notifyDataSetChanged();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
