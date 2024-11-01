package bn.poro.quran;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

public class CompressTask extends Thread {
    private final File imageFile;
    private final Bitmap image;
    private final CompressListener listener;

    public CompressTask(Bitmap bitmap, File file, CompressListener compressListener) {
        imageFile = file;
        image = bitmap;
        listener = compressListener;
    }

    @Override
    public void run() {
        try {
            OutputStream outputStream = Files.newOutputStream(imageFile.toPath());
            image.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            new Handler(Looper.getMainLooper()).post(() -> listener.onCompressed(imageFile));
        } catch (Exception e) {
            L.d(e);
        }
    }

    public interface CompressListener {
        void onCompressed(File file);
    }
}
