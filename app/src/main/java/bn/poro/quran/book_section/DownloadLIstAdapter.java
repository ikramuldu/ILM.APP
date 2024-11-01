package bn.poro.quran.book_section;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;

import java.io.File;

import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

public class DownloadLIstAdapter extends BaseAdapter implements View.OnClickListener {
    private final Item[] items;
    private final Activity activity;

    public DownloadLIstAdapter(Activity activity) {
        this.activity = activity;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,text from books", null);
        items = new Item[cursor.getCount()];
        for (int i = 0; i < items.length; i++) {
            cursor.moveToPosition(i);
            items[i] = new Item(cursor.getInt(0), cursor.getString(1));
        }
        cursor.close();
        database.close();
    }

    @Override
    public int getCount() {
        return items.length;
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
        Item item = items[position];
        CompoundButton button;
        if (convertView == null) {
            button = (CompoundButton) LayoutInflater.from(activity).inflate(R.layout.checkbox, parent, false);
            button.setOnClickListener(this);
        } else button = (CompoundButton) convertView;
        button.setText(item.text);
        button.setTag(position);
        button.setChecked(new File(Utils.dataPath + Consts.BOOK_SUB_PATH + item.id).exists());
        return button;
    }

    @Override
    public void onClick(View v) {
        Item item = items[(Integer) v.getTag()];
        File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH, item.text);
        if (((CompoundButton) v).isChecked()) {
            if (!file.exists())
                activity.startService(new Intent(activity, DownloadService.class)
                        .putExtra(Consts.ID_KEY, item.id)
                        .putExtra(Consts.NAME_KEY, item.text)
                        .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + Consts.BOOK_SUB_PATH + item.id + ".zip")
                        .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + item.id)
                        .putExtra(Consts.PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + item.id + ".zip")
                        .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_BOOK));
        } else if (file.exists()) {
            new AlertDialog.Builder(activity)
                    .setMessage(activity.getString(R.string.delete_book,
                            item.text, DownloadRunnable.getString(file.length())))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> new File(Utils.dataPath + Consts.BOOK_SUB_PATH, item.text).delete())
                    .show();
        }
    }

    private static class Item {
        final int id;
        final String text;

        public Item(int id, String text) {
            this.id = id;
            this.text = text;
        }
    }
}
