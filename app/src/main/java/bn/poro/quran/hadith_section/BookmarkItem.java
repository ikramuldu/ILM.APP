package bn.poro.quran.hadith_section;

class BookmarkItem {
    final int book, position, num;
    final long time;
    final String name;

    BookmarkItem(int book, int position, int num, long time, String name) {
        this.book = book;
        this.position = position;
        this.num = num;
        this.time = time;
        this.name = name;
    }
}
