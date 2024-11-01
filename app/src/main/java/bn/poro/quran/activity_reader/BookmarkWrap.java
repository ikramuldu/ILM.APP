package bn.poro.quran.activity_reader;


import com.shockwave.pdfium.PdfDocument;

class BookmarkWrap {
    final byte level;
    boolean isExpanded;
    final PdfDocument.Bookmark bookmark;

    public BookmarkWrap(PdfDocument.Bookmark bookmark, byte level) {
        this.level = level;
        this.bookmark = bookmark;
    }
}
