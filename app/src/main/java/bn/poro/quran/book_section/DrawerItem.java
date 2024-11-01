package bn.poro.quran.book_section;

import android.database.Cursor;

import androidx.annotation.NonNull;

class DrawerItem {
    final int level, id;
    int nextType;
    final String title;
    boolean isExpanded;


    public DrawerItem(@NonNull Cursor cursor, int level) {
        this.level = level;
        title = cursor.getString(1);
        id = cursor.getInt(0);
    }
}
