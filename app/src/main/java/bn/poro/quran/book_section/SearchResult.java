package bn.poro.quran.book_section;

import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class SearchResult {
    static final int NAME_MATCH = 0;
    static final int NO_MATCH = -1;
    final String id, bookName;
    final int total;
    int[] ids, counts;

    SearchResult(String id, String bookName, @NonNull Cursor cursor) {
        this.id = id;
        this.bookName = bookName;
        int count = cursor.getCount();
        if (count == 0) {
            total = bookName == null ? NO_MATCH : NAME_MATCH;
            return;
        }
        ids = new int[count];
        counts = new int[count];
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            ids[i] = cursor.getInt(0);
            counts[i] = cursor.getInt(1);
        }
        total = cursor.getInt(2);
    }

    @NonNull
    @Override
    public String toString() {
        return bookName;
    }

    public SearchResult(Bundle bundle) {
        id = bundle.getString("id");
        bookName = bundle.getString("ti");
        total = bundle.getInt("to");
        ids = bundle.getIntArray("ids");
        counts = bundle.getIntArray("cnt");
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putString("ti", bookName);
        bundle.putInt("to", total);
        bundle.putIntArray("ids", ids);
        bundle.putIntArray("cnt", counts);
        return bundle;
    }
}
