package bn.poro.quran.book_section;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

class SearchTask extends Thread {
    private final Listener listener;
    private final String search;
    private final String path;
    private boolean cancelled;
    private final ArrayList<SearchResult> results;

    SearchTask(String path, String s, Listener listener) {
        search = s;
        this.listener = listener;
        this.path = path;
        results = new ArrayList<>();
    }

    @Override
    public void run() {
        Handler handler = new Handler(Looper.getMainLooper());
        File file = new File(path);
        if (file.isFile()) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("with tbl as (select id,(length(text)-length(replace(text,?,'')))/length(?) as cnt from content) select id,cnt,(select sum(cnt) from tbl) as total from tbl where cnt>0",
                    new String[]{search, search});
            String id = file.getName();
            results.add(new SearchResult(id, null, cursor));
            cursor.close();
            database.close();
            handler.post(() -> listener.onProgress(results));
            return;
        }
        SQLiteDatabase bookDb = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        String s = "%" + search + "%";
        Cursor nameSearch = bookDb.rawQuery("select id from books where text like ? or writer like ?", new String[]{s, s});
        ArrayList<Integer> ids = new ArrayList<>();
        while (nameSearch.moveToNext()) ids.add(nameSearch.getInt(0));
        nameSearch.close();
        Cursor allBookCursor = bookDb.rawQuery("select id,text from books", null);
        long updateTime = System.currentTimeMillis();
        while (allBookCursor.moveToNext()) {
            String id = allBookCursor.getString(0);
            String title = allBookCursor.getString(1);
            file = new File(path + id);
            if (!file.exists()) continue;
            SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("with tbl as (select id,(length(text)-length(replace(text,?,'')))/length(?) as cnt from content) select id,cnt,(select sum(cnt) from tbl) as total from tbl where cnt>0",
                    new String[]{search, search});
            if (cursor.getCount() > 0 || ids.contains(allBookCursor.getInt(0)))
                results.add(new SearchResult(id, title, cursor));
            cursor.close();
            database.close();
            if (cancelled) return;
            long time = System.currentTimeMillis();
            if (results.size() > 1 && time > updateTime) {
                handler.post(() -> listener.onProgress(results));
                updateTime = time + 3000;
            }
        }
        allBookCursor.close();
        bookDb.close();
        handler.post(() -> listener.onProgress(results));
        handler.post(() -> listener.onProgress(null));
    }

    void cancel() {
        cancelled = true;
    }

    interface Listener {
        void onProgress(ArrayList<SearchResult> result);
    }
}
