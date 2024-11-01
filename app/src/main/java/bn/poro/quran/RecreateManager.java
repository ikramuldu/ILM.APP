package bn.poro.quran;

public class RecreateManager {
    public static final int MAIN_ACTIVITY = 1;
    public static final int HADIS_READING_ACTIVITY = 1 << 1;
    public static final int HADIS_SEARCH_ACTIVITY = 1 << 2;
    public static final int QURAN_ACTIVITY = 1 << 3;
    public static final int QURAN_SEARCH_ACTIVITY = 1 << 4;
    public static final int BOOK_READING_ACTIVITY = 1 << 5;
    public static final int BOOK_SEARCH_ACTIVITY = 1 << 6;
    public static final int PDF_ACTIVITY = 1 << 7;
    public static final int ISLAMHOUSE_ACTIVITY = 1 << 8;
    private static final int RECREATE_ALL = -1;
    private static int status;

    public static void recreateAll() {
        status = RECREATE_ALL;
    }

    public static boolean needRecreate(int activityFlag) {
        return (status & activityFlag) != 0;
    }

    public static void recreated(int activity) {
        status = status & ~activity;
    }
}
