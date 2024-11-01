package bn.poro.quran.app_updater;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.Codes;
import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.DownloadWithoutProgress;
import bn.poro.quran.L;
import bn.poro.quran.Utils;


public class MyAppUpdater implements InstallStateUpdatedListener, GetInfoTask.InfoListener, DownloadAppTask.DownloadListener, OnSuccessListener<AppUpdateInfo>, OnFailureListener, DownloadWithoutProgress.DownloadListener {
    public static final String INFO_URL = DownloadService.BASE_URL + BuildConfig.APPLICATION_ID + ".json";
    private static final int UPDATE_REQUEST_CODE = 1223;
    private static final String APP_VERSION = "app-version";
    private static final String PLAY_OK = "store-ok";
    private static final String UPDATE_TITLE = "App Update Available";
    private static final String UPDATE_NOTE = "note";
    private static final String UPDATE_READY = "Update Ready";
    private static final String INSTALL_NOW = "Install now";
    private static final String INSTALL_LATER = "later";
    private static final String STORE_LINK = "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID;
    private static final String DOWNLOAD_NOW = "Download now";
    private static final String APK_DIR = "apk";
    private static final String PREF_NAME = "update_info";
    private static final String NEXT_CHECK_TIME = "check_time";
    private static final long UPDATE_INTERVAL = 2 * DAY_IN_MILLIS;
    private static final String DOWNLOAD_MESSAGE = "\nfile saved in downloads folder. Please go to Downloads folder and install it.";
    private final Activity activity;
    private int latestVersion;
    private AppUpdateManager appUpdateManager;

    public MyAppUpdater(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdate() {
        if (!checkOfflineUpdate()) {
            SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (preferences.getLong(NEXT_CHECK_TIME, 0L) < System.currentTimeMillis()) {
                new GetInfoTask(this).start();
                new DownloadWithoutProgress(new File(bn.poro.quran.Utils.dataPath + Consts.FILE_LIST_ZIP), DownloadService.BASE_URL + "zip/" + Consts.FILE_LIST_ZIP, this).start();
            }
        }
    }

    @Override
    public void onSuccess(@NonNull AppUpdateInfo appUpdateInfo) {
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            appUpdateManager.completeUpdate();
        } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            try {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), UPDATE_REQUEST_CODE);
                appUpdateManager.registerListener(this);
            } catch (Exception e) {
                new AlertDialog.Builder(activity)
                        .setTitle(UPDATE_TITLE)
                        .setCancelable(false)
                        .setNegativeButton(INSTALL_LATER, (dialog, which) -> setNextUpdateTime()).setPositiveButton(DOWNLOAD_NOW, (dialog, which) ->
                                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_LINK))))
                        .show();
            }
        }
    }


    @Override
    public void onStateUpdate(@NonNull InstallState state) {
        L.d("install status:" + state.installStatus());
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            new AlertDialog.Builder(activity)
                    .setTitle(UPDATE_READY)
                    .setCancelable(false)
                    .setPositiveButton(INSTALL_NOW, (dialog, which) -> appUpdateManager.completeUpdate())
                    .setNegativeButton(INSTALL_LATER, null).show();
        }
    }

    @Override
    public void onInfoReady(@Nullable String data) {
        try {
            JSONObject info = new JSONObject(data);
            latestVersion = info.getInt(APP_VERSION);
            if (info.getBoolean(PLAY_OK)) {
                checkPlayStore();
            } else if (latestVersion > BuildConfig.VERSION_CODE) {
                new AlertDialog.Builder(activity)
                        .setTitle(UPDATE_TITLE)
                        .setMessage(info.getString(UPDATE_NOTE))
                        .setCancelable(false)
                        .setNegativeButton(INSTALL_LATER, (dialog, which) -> setNextUpdateTime()).setPositiveButton(DOWNLOAD_NOW, (dialog, which) -> downloadApk())
                        .show();
            } else {
                clearApkCache();
                setNextUpdateTime();
            }
        } catch (Exception e) {
            checkPlayStore();
            L.d("info error");
        }
    }

    private void downloadApk() {
        String fileName = BuildConfig.APPLICATION_ID + "-" + latestVersion + ".apk";
        String url = DownloadService.BASE_URL + "apk/" + fileName;
        File apkDir = new File(activity.getCacheDir(), APK_DIR);
        if (!apkDir.exists()) apkDir.mkdir();
        new DownloadAppTask(url, new File(apkDir, fileName), this).start();
        Toast.makeText(activity, "downloading update", Toast.LENGTH_SHORT).show();
    }

    private void checkPlayStore() {
        appUpdateManager = AppUpdateManagerFactory.create(activity);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(this).addOnFailureListener(this);
    }

    private void clearApkCache() {
        File apkDir = new File(activity.getCacheDir(), APK_DIR);
        String[] list = apkDir.list();
        if (list != null) for (String s : list) new File(apkDir, s).delete();
    }

    @Override
    public void onDownloadCompleted(File file) {
        PackageInfo apkInfo = activity.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), 0);
        if (apkInfo == null) {
            file.delete();
            Toast.makeText(activity, "invalid apk", Toast.LENGTH_SHORT).show();
        } else installApk(file, apkInfo);
    }

    private File copyToExternalStorage(File file, String externalName) {
        File externalFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), externalName);
        try {
            Utils.copy(file, externalFile);
        } catch (Exception e) {
            L.d(e);
        }
        return externalFile;
    }

    private void openFolder() {
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
        Intent folderIntent = new Intent(Intent.ACTION_VIEW);
        folderIntent.setDataAndType(uri, "resource/folder");
        Intent downloadsIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        ArrayList<OpenableApp> openableApps = listAppFor(folderIntent, downloadsIntent);
        if (openableApps.isEmpty()) openInAnyApp(folderIntent, downloadsIntent);
        else if (openableApps.size() == 1) openableApps.get(0).open(activity);
        else createChooser(openableApps);
    }

    private void createChooser(ArrayList<OpenableApp> apps) {
        AppsAdapter adapter = new AppsAdapter(apps);
        new AlertDialog.Builder(activity).
                setTitle("Open in")
                .setCancelable(false)
                .setAdapter(adapter, adapter)
                .show();
    }

    private ArrayList<OpenableApp> listAppFor(Intent... intents) {
        ArrayList<OpenableApp> openableApps = new ArrayList<>();
        for (Intent intent : intents) {
            List<ResolveInfo> info1 = activity.getPackageManager().queryIntentActivities(intent, 0);
            for (ResolveInfo info : info1) {
                if (hasInstallPermission(info.activityInfo.packageName))
                    openableApps.add(new OpenableApp(info.activityInfo.applicationInfo, intent));
            }
        }
        return openableApps;
    }

    private void openInAnyApp(Intent... intents) {
        for (Intent intent : intents)
            try {
                activity.startActivity(intent);
                break;
            } catch (Exception e) {
                L.d(e);
            }
    }

    private boolean hasInstallPermission(String packageName) {
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null)
                for (String permission : packageInfo.requestedPermissions)
                    if (permission.equals(Manifest.permission.REQUEST_INSTALL_PACKAGES))
                        return true;
        } catch (Exception e) {
            L.d(e);
        }
        return false;
    }

    private boolean checkOfflineUpdate() {
        File apkDir = new File(activity.getCacheDir(), APK_DIR);
        String[] names = apkDir.list();
        PackageManager packageManager = activity.getPackageManager();
        if (names != null) for (String name : names) {
            int apkVersion = getVersionFromFileName(name);
            File apkFile = new File(apkDir, name);
            if (apkVersion <= BuildConfig.VERSION_CODE) {
                apkFile.delete();
                continue;
            }
            PackageInfo apkInfo = packageManager.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (apkInfo != null) {
                installApk(apkFile, apkInfo);
                return true;
            }
        }
        return false;
    }

    private void installApk(File apkFile, PackageInfo apkInfo) {
        //up to 22 copy to external
        //23 external after request write permission
        //24,25 install via provider
        //26-28 same as 23
        //29-32 read external
        PackageManager packageManager = activity.getPackageManager();
        String externalName = activity.getApplicationInfo().loadLabel(packageManager) + "-v" + apkInfo.versionName + ".apk";
        if (Build.VERSION.SDK_INT <= 22) {
            promptInstall(copyToExternalStorage(apkFile, externalName));
            return;
        }
        boolean hasPermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT < 26)
            try {
                boolean installAllowed = Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
                L.d("Install: " + installAllowed);
                if (!installAllowed) {
                    new AlertDialog.Builder(activity)
                            .setMessage("Go to security settings and turn ON \"Install from Unknown source\"")
                            .setPositiveButton("Open Settings", (dialog, which) -> activity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)))
                            .setCancelable(false)
                            .show();
                    return;
                }
            } catch (Exception e) {
                L.d(e);
            }
        if (Build.VERSION.SDK_INT == 23) {
            if (hasPermission) {
                promptInstall(copyToExternalStorage(apkFile, externalName));
            } else requestPermission();
            return;
        }
        if (Build.VERSION.SDK_INT <= 25) {
            if (!installUsingProvider(apkFile)) {
                if (hasPermission) {
                    if (!promptInstall(copyToExternalStorage(apkFile, externalName)))
                        showInstallInstruction(externalName);
                } else requestPermission();
            }
            return;
        }
        if (Build.VERSION.SDK_INT <= 28) {
            if (hasPermission) {
                copyToExternalStorage(apkFile, externalName);
                showInstallInstruction(externalName);
            } else {
                requestPermission();
            }
            return;
        }
        if (copyUsingContentResolver(apkFile, externalName))
            showInstallInstruction(externalName);
    }

    @SuppressLint("NewApi")
    private void requestPermission() {
        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Codes.STORAGE_PERMISSION_REQUEST);
    }

    private boolean promptInstall(File file) {
        Uri apkUri = Uri.fromFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        if (intent.resolveActivity(activity.getPackageManager()) != null) try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            L.d(e);
        }
        return false;
    }

    private boolean installUsingProvider(File file) {
        Uri apkUri = FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setData(apkUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(activity.getPackageManager()) != null) try {
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            L.d(e);
        }
        return false;
    }

    private void showInstallInstruction(String externalName) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(externalName);
        stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new RelativeSizeSpan(1.2f), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new BackgroundColorSpan(Color.GREEN), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new ForegroundColorSpan(Color.BLACK), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.append(DOWNLOAD_MESSAGE);
        new AlertDialog.Builder(activity)
                .setTitle(UPDATE_READY)
                .setMessage(stringBuilder)
                .setCancelable(false)
                .setNegativeButton(INSTALL_LATER, (dialog, which) -> setNextUpdateTime())
                .setPositiveButton("open folder", (dialog, which) -> openFolder()).show();
    }

    private void setNextUpdateTime() {
        activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putLong(NEXT_CHECK_TIME, System.currentTimeMillis() + UPDATE_INTERVAL).apply();
    }

    private boolean copyUsingContentResolver(File apkFile, String externalName) {
        ContentResolver contentResolver = activity.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, externalName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/apk");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        Uri folderUri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.MediaColumns._ID};
        Cursor cursor = contentResolver.query(folderUri, projection, MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?", new String[]{externalName}, null);
        if (cursor != null) {
            L.d("cursor: " + cursor.getCount());
            int columnIndex = cursor.getColumnIndex(projection[0]);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(columnIndex);
                Uri uri = Uri.parse(folderUri + "/" + id);
                contentResolver.delete(uri, null, null);
                L.d("deleted: " + id);
            }
            cursor.close();
        } else L.d("cursor null");
        Uri inserted = contentResolver.insert(folderUri, contentValues);
        if (inserted == null) {
            L.d("null insert");
            return false;
        }
        try {
            OutputStream outputStream = contentResolver.openOutputStream(inserted, "wt");
            if (outputStream == null) {
                L.d("null output stream");
                return false;
            }
            Utils.copy(apkFile, outputStream);
            return true;
        } catch (Exception e) {
            L.d(e);
        }
        return false;
    }

    public static int getVersionFromFileName(String name) {
        Matcher matcher = Pattern.compile("-(\\d+).apk").matcher(name);
        if (matcher.find()) {
            String s = matcher.group(1);
            if (s != null)
                return Integer.parseInt(s);
        }
        return 0;
    }

    public void onRequestPermissionsResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkOfflineUpdate();
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        L.d(e);
    }

    @Override
    public void onDownloaded(File file, int code) {
        if (code != DownloadRunnable.COMPLETED) return;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY)) {
            db.rawQuery("select id from trans limit 1", null).close();
            Utils.move(file, new File(Utils.dataPath + Consts.FILE_LIST_DB));
            L.d("updated list");
            setNextUpdateTime();
        } catch (Exception e) {
            L.d(e);
        }
    }
}
