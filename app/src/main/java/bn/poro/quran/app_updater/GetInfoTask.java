package bn.poro.quran.app_updater;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import bn.poro.quran.L;
import bn.poro.quran.Utils;

public class GetInfoTask extends Thread {
    private final InfoListener listener;
    private String responseText;

    GetInfoTask(InfoListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(MyAppUpdater.INFO_URL).openConnection();
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Utils.copy(connection.getInputStream(), outputStream);
                outputStream.close();
                responseText = outputStream.toString("UTF-8");
            }
            connection.disconnect();
        } catch (UnknownHostException ignored) {
        } catch (Exception e) {
            L.d(e);
        }
        new Handler(Looper.getMainLooper()).post(() -> listener.onInfoReady(responseText));
    }

    interface InfoListener {
        void onInfoReady(@Nullable String s);
    }
}
