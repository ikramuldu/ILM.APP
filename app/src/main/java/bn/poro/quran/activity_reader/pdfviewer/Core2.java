package bn.poro.quran.activity_reader.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.view.Surface;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;

import java.util.List;

public class Core2 extends PdfiumCore {

    public Core2(Context ctx) {
        super(ctx);
    }

    @Override
    public int getPageCount(PdfDocument doc) {
        return super.getPageCount(doc) * 2;
    }

    @Override
    public long openPage(PdfDocument doc, int pageIndex) {
        return super.openPage(doc, pageIndex / 2);
    }

    @Override
    public long[] openPage(PdfDocument doc, int fromIndex, int toIndex) {
        return super.openPage(doc, fromIndex / 2, toIndex / 2);
    }

    @Override
    public int getPageWidth(PdfDocument doc, int index) {
        return super.getPageWidth(doc, index / 2) / 2;
    }

    @Override
    public int getPageHeight(PdfDocument doc, int index) {
        return super.getPageHeight(doc, index / 2);
    }

    @Override
    public int getPageWidthPoint(PdfDocument doc, int index) {
        return super.getPageWidthPoint(doc, index / 2) / 2;
    }


    @Override
    public Size getPageSize(PdfDocument doc, int index) {
        Size size = super.getPageSize(doc, index / 2);
        return new Size(size.getWidth() / 2, size.getHeight());
    }

    @Override
    public void renderPage(PdfDocument doc, Surface surface, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPage(doc, surface, pageIndex, startX, startY, drawSizeX, drawSizeY, true);
    }

    @Override
    public void renderPage(PdfDocument doc, Surface surface, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY, boolean renderAnnot) {
        super.renderPage(doc, surface, pageIndex / 2, startX, startY, drawSizeX * 2, drawSizeY, renderAnnot);
    }

    @Override
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPageBitmap(doc, bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    @Override
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex, int startX, int startY, int drawSizeX, int drawSizeY, boolean renderAnnot) {
        if ((pageIndex & 1) == 0)
            super.renderPageBitmap(doc, bitmap, pageIndex / 2, startX, startY, drawSizeX + drawSizeX, drawSizeY, renderAnnot);
        else
            super.renderPageBitmap(doc, bitmap, pageIndex / 2, startX - drawSizeX, startY, drawSizeX + drawSizeX, drawSizeY, renderAnnot);
    }

    @Override
    public List<PdfDocument.Link> getPageLinks(PdfDocument doc, int pageIndex) {
        return super.getPageLinks(doc, pageIndex / 2);
    }

    @Override
    public RectF mapRectToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX, int sizeY, int rotate, RectF coords) {
        return super.mapRectToDevice(doc, pageIndex / 2, startX, startY, sizeX, sizeY, rotate, coords);
    }
}
