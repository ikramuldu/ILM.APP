package bn.poro.quran.activity_main;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.Handler;
import android.os.Looper;

import java.io.File;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.Utils;

class InitTask extends Thread {

    private final MainActivity activity;

    InitTask(MainActivity activity) {
        this.activity = activity;
    }

    void initStartIndex() {
        activity.startIndexOfSura = new int[Consts.SURA_COUNT];
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select text from quran where ayah is null", null);
        int index = 0;
        for (int i = 0; i < Consts.SURA_COUNT; i++) {
            cursor.moveToPosition(i);
            activity.startIndexOfSura[i] = index;
            index += cursor.getInt(0) + 1;
        }
        cursor.close();
        database.close();
    }

    @Override
    public void run() {
        new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + "1").mkdirs();
        new File(Utils.dataPath + Consts.THUMB_DIR).mkdir();
        new File(Utils.dataPath + Consts.TEMP_WORD_DIR).mkdir();
        new File(Utils.dataPath + Consts.BOOK_SUB_PATH).mkdir();
        new File(Utils.dataPath + Consts.HADIS_SUB_PATH).mkdir();
        File bookmarkFile = new File(Utils.dataPath + Consts.BOOKMARK_FILE);
        if (bookmarkFile.exists()) checkBookmark();
        else copyBookmarkFile();
        SQLiteDatabase bookmarkDB = SQLiteDatabase.openDatabase(bookmarkFile.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        if (preferences.getInt(Consts.DB_VERSION_KEY, 0) < Consts.DATABASE_VERSION) try {
            Utils.copyFromAssets(activity, Consts.ARABIC_DB);
            Utils.copyFromAssets(activity, Consts.FILE_LIST_DB);
            Utils.copyFromAssets(activity, Consts.PLACE_DB);
            Utils.copyFromAssets(activity, Consts.QURAN_DB_NAME);
            Utils.copyFromAssets(activity, "5000.db");
            bookmarkDB.execSQL("insert or ignore into status values (5000,1,1)");
            Utils.copyFromAssets(activity, "5120.db");
            bookmarkDB.execSQL("insert or ignore into status values (5120,1,1)");
            Utils.copyFromAssets(activity, "5204.db");
            bookmarkDB.execSQL("insert or ignore into status values (5204,1,0)");
            preferences.edit().putInt(Consts.DB_VERSION_KEY, Consts.DATABASE_VERSION).apply();
        } catch (Exception e) {
            L.d(e);
        }

        checkDB(Consts.ARABIC_DB);
        checkDB(Consts.FILE_LIST_DB);
        checkDB(Consts.PLACE_DB);
        checkDB(Consts.QURAN_DB_NAME);

        Cursor ids = bookmarkDB.rawQuery("select id from status", null);
        while (ids.moveToNext()) {
            File file = new File(Utils.dataPath + ids.getString(0) + ".db");
            try {
                SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor cursor = database.rawQuery("select name from sqlite_master limit 1", null);
                cursor.close();
                database.close();
            } catch (Exception e) {
                Utils.deleteDB(file);
                bookmarkDB.execSQL("delete from status where id=" + ids.getString(0));
            }
        }
        ids.close();
        bookmarkDB.close();
        initStartIndex();
        new Handler(Looper.getMainLooper()).post(activity::onInitTaskFinish);
    }

    private void checkDB(String fileName) {
        File file = new File(Utils.dataPath + fileName);
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select name from sqlite_master limit 1", null);
            cursor.close();
            database.close();
        } catch (Exception exception) {
            file.delete();
            try {
                Utils.copyFromAssets(activity, fileName);
            } catch (Exception e) {//todo java.io.IOException: No space left on device
                L.d(e);
            }
        }
    }

    private void checkBookmark() {
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select name from sqlite_master", null);
            if (cursor.getCount() < 10)
                throw new SQLiteDatabaseCorruptException(Consts.BOOKMARK_FILE + " is corrupted. table count: " + cursor.getCount());
            cursor.close();
            database.close();
        } catch (Exception exception) {
            L.d(exception);
            copyBookmarkFile();
        }
    }

    private void copyBookmarkFile() {
        try {
            Utils.copyFromAssets(activity, Consts.BOOKMARK_FILE);
        } catch (Exception e) {
            L.d(e);
        }
    }
}
