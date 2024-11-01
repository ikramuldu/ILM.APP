package bn.poro.quran.hadith_section;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.Utils;

class SearchTask extends Thread {
    private boolean cancelled;
    private ArrayList<HadisResult> results;
    int resultCount;
    private final String highlightText;
    private final int bookId;
    private final HadisSearchTaskListener listener;

    SearchTask(String highlightText, int bookId, HadisSearchTaskListener listener) {
        this.highlightText = highlightText;
        this.bookId = bookId;
        this.listener = listener;

    }

    @Override
    public void run() {
        results = new ArrayList<>();
        if (bookId != -1) search(bookId, "");
        else {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select id,name from hadis", null);
            while (cursor.moveToNext() && !cancelled) try {
                search(cursor.getInt(0), cursor.getString(1));
            } catch (Exception e) {
                L.d(e);
            }
            cursor.close();
            database.close();
        }
        if (!cancelled)
            new Handler(Looper.getMainLooper()).post(() -> listener.onSearchCompleted(results, resultCount));
    }

    private void search(int bookId, String string) {
        File file = new File(Utils.dataPath + Consts.HADIS_SUB_PATH + bookId + ".db");
        if (!file.exists()) return;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        boolean num = Pattern.compile("\\s*[০-৯\\d]+\\s*").matcher(highlightText).matches();
        StringBuilder queryBuilder = new StringBuilder("SELECT group_concat(id),count(id) from content where ");
        if (num) {
            char[] chars = highlightText.trim().toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] > '9') chars[i] = (char) (chars[i] - '০' + '0');
            }
            queryBuilder.append("id=").append(new String(chars));
        } else {
            queryBuilder.append("ar like '%").append(highlightText).append("%'");
            queryBuilder.append("or bn like '%").append(highlightText).append("%'");
        }
        Cursor cursor = database.rawQuery(queryBuilder.toString(), null);
        cursor.getCount();
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(1);
            if (count > 0) {
                results.add(new HadisResult(string, cursor.getString(0), bookId, count));
                resultCount += count;
            }
        }
        cursor.close();
        database.close();
    }

    public void cancel() {
        cancelled = true;
    }

    interface HadisSearchTaskListener {
        void onSearchCompleted(ArrayList<HadisResult> results, int count);
    }
}
