package bn.poro.quran.print;

import android.database.Cursor;

public class TransData {
    final int id;
    final Cursor cursor;
    final String name;

    TransData(int id, String name, Cursor cursor) {
        this.id = id;
        this.name = name;
        this.cursor = cursor;
    }
}
