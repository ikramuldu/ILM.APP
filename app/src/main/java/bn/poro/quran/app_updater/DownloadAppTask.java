package bn.poro.quran.app_updater;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import bn.poro.quran.L;
import bn.poro.quran.Utils;

public class DownloadAppTask extends Thread {
    private static final int HTTP_RANGE_ERROR = 416;
    private final String url;
    private final File file;
    private final DownloadListener listener;

    DownloadAppTask(String url, File file, DownloadListener listener) {
        this.url = url;
        this.file = file;
        this.listener = listener;
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            int size = (int) file.length();
            if (size > 0)
                connection.setRequestProperty("Range", "bytes=" + size + "-");
            int status = connection.getResponseCode();
            if (status == HTTP_RANGE_ERROR) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onDownloadCompleted(file));
            } else if (status == HttpURLConnection.HTTP_PARTIAL || status == HttpURLConnection.HTTP_OK) {
                Utils.copy(connection.getInputStream(), file);
                new Handler(Looper.getMainLooper()).post(() -> listener.onDownloadCompleted(file));
            } else L.d("error: " + status);
        } catch (Exception e) {
            L.d(e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    interface DownloadListener {
        void onDownloadCompleted(File file);
    }
}
