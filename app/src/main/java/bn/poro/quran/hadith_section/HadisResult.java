package bn.poro.quran.hadith_section;

import android.os.Bundle;

public class HadisResult {
    public static final String RESULT_KEY = "res";
    public static final String ID_KEY = "id";
    public static final String NAME_KEY = "name";
    private static final String TOTAL_KEY = "total";
    final String bookName, resultIds;
    final int bookId, total;

    public HadisResult(String bookName, String resultIds, int bookId, int total) {
        this.bookName = bookName;
        this.resultIds = resultIds;
        this.bookId = bookId;
        this.total = total;
    }

    private HadisResult(Bundle bundle) {
        bookName = bundle.getString(NAME_KEY);
        resultIds = bundle.getString(RESULT_KEY);
        bookId = bundle.getInt(ID_KEY);
        this.total = bundle.getInt(TOTAL_KEY);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(NAME_KEY, bookName);
        bundle.putString(RESULT_KEY, resultIds);
        bundle.putInt(ID_KEY, bookId);
        bundle.putInt(TOTAL_KEY, total);
        return bundle;
    }
}
