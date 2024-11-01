package bn.poro.quran.book_section;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

 class CheckExpandabilityTask extends Thread {
    private final DrawerAdapter adapter;
    private int start;
    private final int end;

     CheckExpandabilityTask(DrawerAdapter adapter, int start, int end) {
        this.adapter = adapter;
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
                Utils.dataPath+ Consts.BOOK_SUB_PATH  + adapter.activity.bookID,
                null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        DrawerItem currentItem = adapter.drawerItems.get(start);

        Handler handler = new Handler(Looper.getMainLooper());
        while (start < end) {
            start++;
            DrawerItem nextItem;
            int nextId;
            if (start == adapter.drawerItems.size()) {
                nextId = Integer.MAX_VALUE;
                nextItem = null;
            } else {
                nextItem = adapter.drawerItems.get(start);
                nextId = nextItem.id;
            }
            Cursor cursor = database.rawQuery(
                    "select type from content where id between ? and ? and type>? limit 1",
                    new String[]{String.valueOf(currentItem.id + 1),
                            String.valueOf(nextId - 1),
                            String.valueOf(currentItem.level)});
            if (cursor.moveToFirst()) {
                currentItem.nextType = cursor.getInt(0);
                handler.post(() -> adapter.notifyItemChanged(start - 1, true));
            }
            currentItem = nextItem;
            cursor.close();
        }
        database.close();
    }
}
