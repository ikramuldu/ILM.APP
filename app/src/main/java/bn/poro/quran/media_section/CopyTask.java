package bn.poro.quran.media_section;

import android.os.Handler;
import android.os.Looper;

import java.io.File;

import bn.poro.quran.Utils;

class CopyTask extends Thread {
    final String from;
    final String to;
    final CopyListener listener;

    CopyTask(String from, String to, CopyListener listener) {
        this.from = from;
        this.to = to;
        this.listener = listener;
    }

    @Override
    public void run() {
        copyFiles(new File(from), new File(to));
        new Handler(Looper.getMainLooper()).post(() -> listener.onCopyComplete(from));
    }

    private void copyFiles(File from, File to) {
        File[] files = from.listFiles();
        if (files == null) return;
        to.mkdirs();
        for (File src : files) {
            File dest = new File(to, src.getName());
            if (src.isDirectory()) {
                copyFiles(src, dest);
                src.delete();
            } else Utils.move(src, dest);
        }
    }

    interface CopyListener {
        void onCopyComplete(String from);
    }

}
