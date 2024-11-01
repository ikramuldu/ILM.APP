package bn.poro.quran.activity_quran;

import android.database.sqlite.SQLiteDatabase;

import bn.poro.quran.Consts;
import bn.poro.quran.Utils;

class UpdateRecentTask extends Thread {

    private final int sura, ayah;

    UpdateRecentTask(int sura, int ayah) {
        this.sura = sura;
        this.ayah = ayah;
    }

    @Override
    public void run() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("delete from bookmark where sura = ? and category > ?", new Integer[]{sura, Consts.TIME_MIN});
        database.execSQL("insert into bookmark values(?,?,?)", new Integer[]{sura, ayah,
                (int) (System.currentTimeMillis() / 1000)});
        database.close();
    }
}
