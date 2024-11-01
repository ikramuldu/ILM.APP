package bn.poro.quran.fragments.topic;

import android.database.Cursor;

 class IndexGroupModel {
    final int level;
    final int id;
    final String ayahs;
    final String title;
    boolean isExpanded;


     IndexGroupModel(Cursor cursor, int level) {
        title = cursor.getString(1) + " (" + cursor.getString(2) + ")";
        id = cursor.getInt(0);
        ayahs = cursor.getString(3);
        this.level = level;
    }

}
