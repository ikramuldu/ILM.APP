package bn.poro.quran.hadith_section;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.Utils;

public class CheckHadisDatabasesTask extends Thread {
    @Override
    public void run() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id from hadis", null);
        while (cursor.moveToNext()) {
            File file = new File(Utils.dataPath + Consts.HADIS_SUB_PATH + cursor.getString(0) + ".db");
            if (file.exists())
                try {
                    SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    db.rawQuery("select 1 from sqlite_master", null).close();
                    db.close();
                    L.d(file.getPath()+" ok");
                } catch (Exception e) {
                    file.delete();
                }
        }
        cursor.close();
        database.close();
    }
}
