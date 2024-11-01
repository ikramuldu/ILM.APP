package bn.poro.quran;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.zip.ZipInputStream;

public class DownloadWithoutProgress extends Thread {
    private final String url;
    private final DownloadListener listener;
    private final File file;
    private int status;

    public DownloadWithoutProgress(@NonNull File file, @NonNull String url, @Nullable DownloadListener listener) {
        this.file = file;
        this.url = url;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                File parent = file.getParentFile();
                if (parent == null || !parent.exists()) parent.mkdirs();
                File tempFile = new File(file.getPath() + Consts.LOADING_FILE);
                Utils.copy(connection.getInputStream(), tempFile);
                connection.disconnect();
                if (url.endsWith("zip")) {
                    InputStream inputStream = Files.newInputStream(tempFile.toPath());
                    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                    zipInputStream.getNextEntry();
                    Utils.copy(zipInputStream, file);
                    inputStream.close();
                    tempFile.delete();
                } else tempFile.renameTo(file);
                status = DownloadRunnable.COMPLETED;
            }
        } catch (UnknownHostException e) {
            status = DownloadRunnable.DOWNLOAD_FAILED;
        } catch (Exception e) {
            status = DownloadRunnable.DOWNLOAD_FAILED;
            L.d(e);
        }
        if (listener != null)
            new Handler(Looper.getMainLooper()).post(() -> listener.onDownloaded(file, status));
    }

    public interface DownloadListener {
        void onDownloaded(File file, int code);
    }
}
