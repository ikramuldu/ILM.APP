package bn.poro.quran.hadith_section;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.Utils;

class InitAutoCompleteTask extends Thread {
    private final SearchHistoryListener listener;

    InitAutoCompleteTask(SearchHistoryListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        File file = new File(Utils.dataPath, Consts.SEARCH_HISTORY_FILE);
        ArrayList<String> list = new ArrayList<>();
        if (file.length() != 0) {
            try {
                InputStream inputStream = Files.newInputStream(file.toPath());
                byte[] bytes = new byte[inputStream.available()];
                if (inputStream.read(bytes) > 0) {
                    String[] items = new String(bytes).split("\n");
                    list.addAll(Arrays.asList(items));
                }
                inputStream.close();
            } catch (Exception e) {
                L.d(e);
            }
        }
        new Handler(Looper.getMainLooper()).post(() -> listener.onSearchHistoryReady(list));
    }

    interface SearchHistoryListener {
        void onSearchHistoryReady(ArrayList<String> list);
    }
}
