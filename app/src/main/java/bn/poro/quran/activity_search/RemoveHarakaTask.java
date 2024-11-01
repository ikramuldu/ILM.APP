package bn.poro.quran.activity_search;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

class RemoveHarakaTask extends Thread {
    private final Listener listener;

    RemoveHarakaTask(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("alter table quran add column ar2 text");
        Cursor cursor = database.rawQuery("select rowid,text from quran where ayah is not null", null);
        database.execSQL("begin transaction");
        while (cursor.moveToNext()) {
            database.execSQL("update quran set ar2='"
                    + cursor.getString(1).replaceAll(Consts.HARAKA_REGEX, "")
                    + "' where rowid=" + cursor.getString(0));
        }
        database.execSQL("commit");
        database.execSQL("vacuum");
        cursor.close();
        database.close();
        new Handler(Looper.getMainLooper()).post(listener::onDatabaseReady);
    }

    interface Listener {
        void onDatabaseReady();
    }
}
