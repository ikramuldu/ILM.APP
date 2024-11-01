package bn.poro.quran;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;

import com.github.junrar.Junrar;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadRunnable implements Runnable {
    public static final int STARTING = 0;
    public static final int DOWNLOADING = -1;
    public static final int CANCELLED = -2;
    public static final int DOWNLOAD_FAILED = -3;
    public static final int COMPLETED = -4;
    private static final long UPDATE_INTERVAL = 500;
    private final DownloadProgressListener listener;
    private final String filePath, extractPath;
    Future<?> futureTask;
    public String title;
    public final String url;
    public int status, position;
    public final int id, type;
    public int totalProgress, totalSize;

    public DownloadRunnable(int id, int type, String url, String title, String filePath, String extractPath, DownloadProgressListener listener) {
        this.id = id;
        this.title = title;
        this.extractPath = extractPath;
        this.type = type;
        this.url = url;
        this.filePath = filePath;
        this.listener = listener;
    }

    @SuppressLint("DefaultLocale")
    public static String getString(long bytes) {
        long kb = bytes >> 10;
        if (kb < 1024) return String.format("%d KB", kb);
        float mb = kb / 1024.0f;
        if (mb < 10) return String.format("%.2f MB", mb);
        if (mb < 100) return String.format("%.1f MB", mb);
        if (mb < 1024) return String.format("%d MB", kb >> 10);
        float gb = mb / 1024;
        if (gb < 10) return String.format("%.2f GB", gb);
        if (gb < 100) return String.format("%.1f GB", gb);
        return String.format("%d GB", kb >> 20);
    }

    @Override
    public void run() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> listener.onDownloadProgress(this));
        File tempFile = new File(filePath + Consts.LOADING_FILE);
        totalProgress = (int) tempFile.length();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (totalProgress > 0)
                connection.setRequestProperty("Range", "bytes=" + totalProgress + "-");
            int response = connection.getResponseCode();
            if (response == HttpURLConnection.HTTP_PARTIAL || response == HttpURLConnection.HTTP_OK) {
                status = DOWNLOADING;
                totalSize = totalProgress + connection.getContentLength();
                handler.post(() -> listener.onDownloadProgress(this));
                int len;
                byte[] buffer = new byte[Utils.BUFFER_SIZE];
                InputStream webStream = connection.getInputStream();
                OutputStream outputStream = Files.newOutputStream(tempFile.toPath(), tempFile.exists() ? APPEND : CREATE);
                long time = System.currentTimeMillis();
                int currentProgress = 0;
                int updateThreshold = totalSize / 1000;
                while ((len = webStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                    currentProgress += len;
                    if (currentProgress >= updateThreshold &&
                            System.currentTimeMillis() - time > UPDATE_INTERVAL) {
                        totalProgress += currentProgress;
                        currentProgress = 0;
                        handler.post(() -> listener.onDownloadProgress(this));
                        time = System.currentTimeMillis();
                    }
                    if (status == CANCELLED) {
                        break;
                    }
                }
                outputStream.flush();
                outputStream.close();
                webStream.close();
                connection.disconnect();
                if (status != CANCELLED) {
                    if (extractPath != null) {
                        File extractDir = new File(extractPath);
                        InputStream inputStream = Files.newInputStream(tempFile.toPath());
                        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                        if (extractDir.isDirectory()) {
                            L.d(extractPath);
                            ZipEntry zipEntry;
                            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                                File file = new File(extractDir, zipEntry.getName());
                                if (!file.getCanonicalPath().startsWith(extractPath)) continue;
                                OutputStream fileOutputStream = Files.newOutputStream(file.toPath());
                                while ((len = zipInputStream.read(buffer)) != -1)
                                    fileOutputStream.write(buffer, 0, len);
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                        } else {
                            ZipEntry entry = zipInputStream.getNextEntry();
                            if (entry != null) {
                                title = entry.getName();
                                Utils.copy(zipInputStream, extractDir);
                            } else {
                                File uniqueTempDir = new File(Utils.dataPath + id + "." + type);
                                uniqueTempDir.mkdir();
                                File extractedFile = Junrar.extract(tempFile, uniqueTempDir).get(0);
                                title = extractedFile.getName();
                                extractedFile.renameTo(extractDir);
                                uniqueTempDir.delete();
                            }
                        }
                        zipInputStream.close();
                        inputStream.close();
                        tempFile.delete();
                    } else {
                        Utils.move(tempFile, new File(filePath));
                    }
                    status = COMPLETED;
                }
            } else {
                status = DOWNLOAD_FAILED;
            }
        } catch (UnknownHostException | SocketException e) {
            status = DOWNLOAD_FAILED;
        } catch (Exception e) {
            L.d(e);
            status = DOWNLOAD_FAILED;
        }
        handler.post(() -> listener.onDownloadProgress(this));
    }

    public void cancel() {
        status = CANCELLED;
        futureTask.cancel(false);
    }

    @NonNull
    @Override
    public String toString() {
        return "id: " + id + ",type: " + type + ",url: " + url;
    }

    public CharSequence getString(Context context) {
        switch (status) {
            case DOWNLOADING:
                return (getString(totalProgress)
                        + "/" + getString(totalSize));
            case STARTING:
                return context.getString(R.string.download_processing);
            case COMPLETED:
                return context.getString(R.string.downloaded);
            case CANCELLED:
                return context.getString(R.string.notification_download_canceled);
            default:
                SpannableString spannableString = new SpannableString(context.getString(R.string.error_network_try_again));
                spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return spannableString;
        }
    }

    public interface DownloadProgressListener {
        void onDownloadProgress(DownloadRunnable task);
    }
}
