package bn.poro.quran;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.SparseArray;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.util.ArrayDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bn.poro.quran.activity_main.MainActivity;

public class DownloadService extends Service implements DownloadRunnable.DownloadProgressListener {
    public static final int WORD_START_INDEX = 5000;
    public static final int TRANS_START_INDEX = 5100;
    public static final int TYPE_PDF_BOOK = 5;
    public static final int TYPE_TRANSLATION_AND_WORD = 6;
    public static final int TYPE_ZIP_AUDIO = 7;
    public static final int TYPE_SURA_AUDIO = 8;
    public static final int TYPE_BOOK = 9;
    public static final int TYPE_HADIS = 10;
    public static final int TYPE_DOWNLOAD_ALL_SURA = 11;
    public static final int TYPE_DOWNLOAD_ALL_BOOK = 12;
    public static final int TYPE_LEARNING_AUDIO = 13;
    public static final int TYPE_CANCEL = 14;
    private static final int BOOK_QUEUE_RUNNING = -1;
    public static final int BOOK_QUEUE_STOPPED = -2;
    private static final int TASK_LIMIT = 5;
    public static final String BASE_URL = "https://data.tazkir.top/app/";
    private PendingIntent contentIntent;
    private ThreadPoolExecutor executorService;
    private SparseArray<SparseArray<DownloadRunnable>> taskListArray;
    private ArrayDeque<Integer> suraQueue;
    public static boolean downloadingAllSura;
    public static int bookQueueProgress = BOOK_QUEUE_STOPPED;

    private DownloadRunnable.DownloadProgressListener listener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public void setListener(DownloadRunnable.DownloadProgressListener listener) {
        this.listener = listener;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onCreate() {
        if (Utils.dataPath == null) {
            Utils.dataPath = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE)
                    .getString(Consts.PATH_KEY, null);
        }
        taskListArray = new SparseArray<>();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel("downloader", "downloader", NotificationManager.IMPORTANCE_LOW);
            mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(mChannel);
        }
        contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        suraQueue = new ArrayDeque<>();
        executorService = new ThreadPoolExecutor(TASK_LIMIT, TASK_LIMIT, 2L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executorService.allowCoreThreadTimeOut(true);
        downloadingAllSura = false;
        bookQueueProgress = BOOK_QUEUE_STOPPED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int type = intent.getIntExtra(Consts.TYPE_KEY, 0);
        switch (type) {
            case TYPE_DOWNLOAD_ALL_BOOK:
                if (bookQueueProgress > BOOK_QUEUE_STOPPED)
                    bookQueueProgress = BOOK_QUEUE_STOPPED;
                else {
                    bookQueueProgress = BOOK_QUEUE_RUNNING;
                    checkBookQueue();
                }
                break;
            case TYPE_DOWNLOAD_ALL_SURA:
                if (downloadingAllSura) stopAll();
                else new Handler(Looper.getMainLooper()).post(this::downloadAllSura);
                break;
            default:
                int id = intent.getIntExtra(Consts.ID_KEY, -1);
                String url = intent.getStringExtra(Consts.URL_KEY);
                if (id == -1) L.e("downloadService: task id is -1");
                DownloadRunnable task = findTask(type, id);
                if (task != null) {
                    if (url == null)
                        task.cancel();
                    break;
                }
                if (url == null) break;
                String name = intent.getStringExtra(Consts.NAME_KEY);
                String path = intent.getStringExtra(Consts.PATH_KEY);
                String extractedPath = intent.getStringExtra(Consts.EXTRACTION_PATH_KEY);
                task = new DownloadRunnable(id, type, url, name, path, extractedPath, this);
                try {
                    task.futureTask = executorService.submit(task);
                } catch (Exception e) {
                    L.d(e);
                    break;
                }
                SparseArray<DownloadRunnable> taskList = taskListArray.get(type);
                if (taskList == null) {
                    taskList = new SparseArray<>();
                    taskListArray.put(type, taskList);
                }
                taskList.put(id, task);
                break;
        }
        return START_NOT_STICKY;
    }

    public DownloadRunnable findTask(int type, int id) {
        SparseArray<DownloadRunnable> array = taskListArray.get(type);
        if (array == null) return null;
        return array.get(id);
    }

    private void stopAll() {
        downloadingAllSura = false;
        suraQueue.clear();
        SparseArray<DownloadRunnable> taskList = taskListArray.get(TYPE_SURA_AUDIO);
        if (taskList == null) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        for (int i = 0; i < taskList.size(); i++) {
            DownloadRunnable task = taskList.valueAt(i);
            task.cancel();
            manager.cancel(String.valueOf(task.type), task.id);
            if (listener != null) listener.onDownloadProgress(task);
        }
    }

    private void downloadAllSura() {
        downloadingAllSura = true;
        SparseArray<DownloadRunnable> taskList = taskListArray.get(TYPE_SURA_AUDIO);
        if (taskList == null) {
            taskList = new SparseArray<>();
            taskListArray.put(TYPE_SURA_AUDIO, taskList);
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select sura,text from quran where ayah is null", null);
        for (int i = 0; i < cursor.getCount(); i++) {
            int sura = i + 1;
            if (taskList.get(sura) != null)
                continue;
            File file = new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura);
            String[] strings = file.list();
            cursor.moveToPosition(i);
            if (file.exists() && strings != null && strings.length == cursor.getInt(1))
                continue;
            if (taskList.size() >= TASK_LIMIT)
                suraQueue.add(sura);
            else {
                DownloadRunnable task = downloadSura(sura);
                if (task != null && listener != null)
                    listener.onDownloadProgress(task);
            }
        }
        cursor.close();
    }

    @org.jetbrains.annotations.Nullable
    private DownloadRunnable downloadSura(int sura) {
        String[] suraNames = getResources().getStringArray(R.array.sura_transliteration);
        File dir = new File(Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura);
        dir.mkdir();
        DownloadRunnable task = new DownloadRunnable(sura, TYPE_SURA_AUDIO, DownloadService.BASE_URL + Consts.QURAN_AUDIO_SUB_PATH + sura + ".zip", suraNames[sura - 1], Utils.dataPath + Consts.QURAN_AUDIO_SUB_PATH + sura + ".zip", dir.getPath(), this);
        try {
            task.futureTask = executorService.submit(task);
        } catch (Exception e) {
            L.d(e);
            return null;
        }
        taskListArray.get(TYPE_SURA_AUDIO).put(sura, task);
        return task;
    }


    @Override
    public void onDestroy() {
        for (int j = 0; j < taskListArray.size(); j++) {
            SparseArray<DownloadRunnable> taskList = taskListArray.valueAt(j);
            if (taskList == null) continue;
            for (int i = 0; i < taskList.size(); i++)
                taskList.valueAt(i).cancel();
            taskList.clear();
        }
        executorService.shutdown();
        downloadingAllSura = false;
        super.onDestroy();
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onDownloadProgress(DownloadRunnable task) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat
                .Builder(this, "downloader")
                .setShowWhen(false)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(task.title)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        SparseArray<DownloadRunnable> taskList = taskListArray.get(task.type);
        if (taskList != null)
            switch (task.status) {
                case DownloadRunnable.DOWNLOADING:
                    notificationBuilder.setSubText(String.format("%.1f%%", (((float) task.totalProgress) / task.totalSize) * 100));
                    notificationBuilder.setProgress(task.totalSize >> 10,
                            task.totalProgress >> 10, false);
                    manager.notify(String.valueOf(task.type), task.id, notificationBuilder.build());
                    break;
                case DownloadRunnable.STARTING:
                    notificationBuilder.setSubText(getString(R.string.loading));
                    notificationBuilder.setProgress(100, 0, true);
                    manager.notify(String.valueOf(task.type), task.id, notificationBuilder.build());
                    break;
                case DownloadRunnable.COMPLETED:
                    taskList.remove(task.id);
                    manager.cancel(String.valueOf(task.type), task.id);
                    Toast.makeText(DownloadService.this, R.string.download_successful, Toast.LENGTH_SHORT).show();
                    if (task.type == TYPE_LEARNING_AUDIO) {
                        SharedPreferences preferences = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
                        preferences.edit().putBoolean(Consts.LEARNING_AUDIO_DOWNLOADED, true).apply();
                    }
                    SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    if (task.type == TYPE_TRANSLATION_AND_WORD) {
                        database.execSQL("insert into status values (?,?,0)", new Object[]{task.id, task.title});
                    } else if (task.type == TYPE_HADIS) {
                        database.execSQL("insert into hadis_status values (?,?)", new Object[]{task.id, task.title});
                    }
                    database.close();
                    if (downloadingAllSura)
                        checkSuraQueue();
                    if (bookQueueProgress != BOOK_QUEUE_STOPPED)
                        checkBookQueue();
                    break;
                case DownloadRunnable.CANCELLED:
                    taskList.remove(task.id);
                    manager.cancel(String.valueOf(task.type), task.id);
                    if (downloadingAllSura)
                        checkSuraQueue();
                    if (bookQueueProgress != BOOK_QUEUE_STOPPED)
                        checkBookQueue();
                    break;
                default:
                    taskList.remove(task.id);
                    Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
                    if (downloadingAllSura) stopAll();
                    manager.cancel(String.valueOf(task.type), task.id);
            }
        if (listener != null) listener.onDownloadProgress(task);
    }

    private void checkSuraQueue() {
        SparseArray<DownloadRunnable> taskList = taskListArray.get(TYPE_SURA_AUDIO);
        if (taskList == null) {
            taskList = new SparseArray<>();
            taskListArray.put(TYPE_SURA_AUDIO, taskList);
        }
        while (taskList.size() < TASK_LIMIT) {
            Integer sura = suraQueue.poll();
            if (sura == null) break;
            downloadSura(sura);
        }
    }

    private void checkBookQueue() {
        SparseArray<DownloadRunnable> taskList = taskListArray.get(TYPE_BOOK);
        if (taskList == null) {
            taskList = new SparseArray<>();
            taskListArray.put(TYPE_BOOK, taskList);
        }
        if (taskList.size() >= TASK_LIMIT) return;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,text from books where id>" + bookQueueProgress, null);
        while (cursor.moveToNext()) {
            File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH, cursor.getString(0));
            if (file.exists()) {
                bookQueueProgress = cursor.getInt(0);
                continue;
            }
            if (taskList.size() >= TASK_LIMIT) break;
            bookQueueProgress = cursor.getInt(0);
            String relativePath = Consts.BOOK_SUB_PATH + bookQueueProgress + ".zip";
            DownloadRunnable task = new DownloadRunnable(bookQueueProgress, TYPE_BOOK, DownloadService.BASE_URL + relativePath, cursor.getString(1), Utils.dataPath + relativePath, file.getPath(), this);
            try {
                task.futureTask = executorService.submit(task);
            } catch (Exception e) {
                L.d(e);
                break;
            }
            taskList.put(bookQueueProgress, task);
        }
        cursor.close();
        database.close();
    }

    public class MyBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }
    }
}
