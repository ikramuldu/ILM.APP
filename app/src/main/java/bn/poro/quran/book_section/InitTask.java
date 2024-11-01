package bn.poro.quran.book_section;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.github.junrar.Junrar;

import java.io.File;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

class InitTask extends Thread {

    private static final int INCLUDED_BOOKS = 68;
    private final Activity activity;
    private final Listener listener;
    private SparseArray<String> mainList;

    InitTask(@NonNull Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    @Override
    public void run() {
        //extractBooks();
        checkBooks();
        new Handler(Looper.getMainLooper()).post(() -> listener.onDatabaseCheckFinish(mainList));
    }

    private void extractBooks() {
        File dir = new File(Utils.dataPath, Consts.BOOK_SUB_PATH);
        String[] strings = dir.list();
        if (strings == null || strings.length < INCLUDED_BOOKS)
            try {
                Junrar.extract(activity.getAssets().open("books.rar"), dir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    private void checkBooks() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor namesCursor = database.rawQuery("select id,text||'\n'||writer from books", null);
        mainList = new SparseArray<>();
        while (namesCursor.moveToNext()) {
            int id = namesCursor.getInt(0);
            mainList.append(id, namesCursor.getString(1));
            File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH + id);
            if (file.exists())
                try {
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    db.rawQuery("select 1 from content limit 1", null).close();
                    db.close();
                } catch (Exception e) {
                    file.delete();
                }
        }
        namesCursor.close();
        database.close();
    }

    interface Listener {
        void onDatabaseCheckFinish(SparseArray<String> names);
    }
}
