package bn.poro.quran.media_section;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

import bn.poro.quran.L;

public class CreateThumb extends Thread {
    final int width;
    final PdfiumCore pdfiumCore;
    final int id, type;
    final File file, thumb;
    final OnCreateThumbListener listener;

    public CreateThumb(Context context, OnCreateThumbListener thumbListener, int id, int type, File file, File thumb) {
        super();
        width = (int) (150 * context.getResources().getDisplayMetrics().density);
        pdfiumCore = new PdfiumCore(context);
        this.id = id;
        this.type = type;
        listener = thumbListener;
        this.file = file;
        this.thumb = thumb;
    }

    @Override
    public void run() {
        try {
            Bitmap bitmap;
            if (type == 2)
                bitmap = ThumbnailUtils.createVideoThumbnail(file.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
            else if (type == 3) {
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(file.getPath());
                byte[] buffer = metadataRetriever.getEmbeddedPicture();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    metadataRetriever.close();
                } else metadataRetriever.release();
                if (buffer == null) return;
                bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
            } else {
                PdfDocument pdfDocument = pdfiumCore.newDocument(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
                pdfiumCore.openPage(pdfDocument, 0);
                int height = width * pdfiumCore.getPageHeight(pdfDocument, 0) / pdfiumCore.getPageWidth(pdfDocument, 0);
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, 0, 0, 0, width, height);
                pdfiumCore.closeDocument(pdfDocument);
            }
            if (bitmap == null) return;
            OutputStream outputStream = Files.newOutputStream(thumb.toPath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            new Handler(Looper.getMainLooper()).post(() -> listener.onThumbCreated(id, thumb.getPath()));
        } catch (Exception e) {
            L.d(e);
        }
    }

    public interface OnCreateThumbListener {
        void onThumbCreated(int id, String path);
    }
}
