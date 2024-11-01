package bn.poro.quran.activity_search;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import java.util.ArrayList;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

class SearchTask extends Thread {

    private final Listener listener;
    private final String search;
    private boolean cancelled;

    SearchTask(String s, Listener listener) {
        search = "%" + s + "%";
        this.listener = listener;
    }

    @Override
    public void run() {
        SQLiteDatabase listDb = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        listDb.execSQL("ATTACH DATABASE ? AS db2", new String[]{Utils.dataPath + Consts.FILE_LIST_DB});
        Cursor listCursor = listDb.rawQuery("select status.id,lang||'-'||name,extra from trans " +
                "inner join status using (id) where status.ver>0", null);
        boolean[] exists = new boolean[Consts.ITEM_COUNT + 1];
        ArrayList<SearchResult> results = new ArrayList<>();
        while (listCursor.moveToNext()) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + listCursor.getString(0) + ".db",
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select rowid,text from content where text like ?", new String[]{search});
            if (cursor.getCount() != 0) {
                SparseArray<String> trans = new SparseArray<>();
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(0);
                    exists[id] = true;
                    trans.append(id, cursor.getString(1));
                }
                results.add(new SearchResult(listCursor.getInt(0), listCursor.getString(1), trans));
            }
            cursor.close();
            database.close();
            if (cancelled) {
                listCursor.close();
                listDb.close();
                return;
            }
        }
        listCursor.close();
        listDb.close();
        //search in quran
        SQLiteDatabase quranDb = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = quranDb.rawQuery("select rowid from quran where text like ? or ar2 like ?", new String[]{search, search});
        while (cursor.moveToNext()) {
            exists[cursor.getInt(0)] = true;
        }
        cursor.close();

        StringBuilder include = new StringBuilder("0");
        for (int i = 1; i <= Consts.ITEM_COUNT; i++) {
            if (exists[i]) {
                include.append(",");
                include.append(i);
            }
        }
        if (cancelled) {
            quranDb.close();
            return;
        }
        Cursor mainCursor = quranDb.rawQuery("SELECT sura,ayah,text,rowid from quran where rowid in (" + include + ")", null);
        ResultItem[] mainItems = new ResultItem[mainCursor.getCount()];
        for (int i = 0; i < mainItems.length; i++) {
            mainCursor.moveToPosition(i);
            mainItems[i] = new ResultItem(mainCursor.getInt(0), mainCursor.getInt(1), mainCursor.getString(2), mainCursor.getInt(3));
        }
        mainCursor.close();
        quranDb.close();
        if (cancelled) return;
        new Handler(Looper.getMainLooper()).post(() -> listener.onFinish(results, mainItems));
    }

    void cancel() {
        cancelled = true;
    }

    interface Listener {
        void onFinish(ArrayList<SearchResult> results, ResultItem[] mainCursor);
    }

    public static class SearchResult {
        final String title;
        final SparseArray<String> trans;
        public final int id;

        SearchResult(int id, String title, SparseArray<String> trans) {
            this.id = id;
            this.title = title;
            this.trans = trans;
        }
    }

    public static class ResultItem {
        final int sura, ayah, id;
        final String text;

        public ResultItem(int sura, int ayah, String text, int id) {
            this.sura = sura;
            this.ayah = ayah;
            this.text = text;
            this.id = id;
        }
    }
}
