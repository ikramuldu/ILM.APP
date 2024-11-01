package bn.poro.quran.activity_main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.transition.Slide;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.Codes;
import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.app_updater.MyAppUpdater;
import bn.poro.quran.fragments.homepage.MainFragment;
import bn.poro.quran.fragments.prayer_time.LocationDecoder;
import bn.poro.quran.fragments.prayer_time.PrayerTimeFragment;
import bn.poro.quran.fragments.qibla_direction.DirectionFragment;
import bn.poro.quran.fragments.setting_main.DriveAdapter;
import bn.poro.quran.fragments.setting_main.LangAdapter;

public class MainActivity extends AppCompatActivity implements LocationListener {

    public int secondaryColor;
    public int[] startIndexOfSura;
    private String[] suraNames;
    private ActivityResultLauncher<Intent> alarmPermissionLauncher, fullscreenPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.MAIN_ACTIVITY);
        final SharedPreferences preferences = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        int themeId = preferences.getInt(Consts.THEME_KEY, -1);
        if (themeId == -1) {
            themeId = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES ? 1 : 0;
            preferences.edit().putInt(Consts.THEME_KEY, themeId).apply();
        }
        setTheme(SettingActivity.THEME[themeId]);
        setContentView(R.layout.fragment_container);
        if (getAppLocale() == null) {
            String oldLanguage = preferences.getString(Consts.LANGUAGE_CODE, null);
            if (oldLanguage == null) {
                preferences.edit().putString(Consts.LANGUAGE_CODE, "").apply();
                LangAdapter adapter = new LangAdapter(this);
                new AlertDialog.Builder(this).setTitle(R.string.pref_settings_choose_language)
                        .setAdapter(adapter, adapter)
                        .setOnCancelListener(adapter)
                        .show();
                return;
            } else if (!oldLanguage.isEmpty()) {
                preferences.edit().putString(Consts.LANGUAGE_CODE, "").apply();
                if (!oldLanguage.equalsIgnoreCase("auto")) {
                    LangAdapter.setLang(oldLanguage);
                    return;
                }
            }
        }
        if (cantUseFullScreenIntent())
            fullscreenPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (cantUseFullScreenIntent())
                    Toast.makeText(MainActivity.this, R.string.permission_req, Toast.LENGTH_SHORT).show();
            });
        if (cantScheduleAlarm())
            alarmPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cantUseFullScreenIntent()) launchFullScreenRequest();
                } else
                    Toast.makeText(MainActivity.this, R.string.permission_req, Toast.LENGTH_SHORT).show();
            });
        onLangReady();
    }

    public void onLangReady() {
        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.tab_text});
        secondaryColor = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else
            typedArray.recycle();
        HashSet<String> pathSet = new HashSet<>();
        File[] files = getExternalFilesDirs(null);
        for (File file : files) if (file != null){
            if (!file.exists() && !file.mkdirs()) continue;
            pathSet.add(file.getPath() + File.separator);
        }
        final SharedPreferences preferences = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        Utils.dataPath = preferences.getString(Consts.PATH_KEY, null);
        if (Utils.dataPath != null && pathSet.contains(Utils.dataPath)) {
            onDataPathReady();
            return;
        }
        if (pathSet.size() == 1) {
            Utils.dataPath = pathSet.iterator().next();
            preferences.edit().putString(Consts.PATH_KEY, Utils.dataPath).apply();
            onDataPathReady();
            return;
        }

        String[] drives = new String[pathSet.size()];
        Iterator<String> iterator = pathSet.iterator();
        for (int i = 0; i < drives.length; i++) {
            drives[i] = iterator.next();
            new File(drives[i]).mkdirs();
        }
        new AlertDialog.Builder(this).setTitle(R.string.prefs_app_location_title).setCancelable(false).setAdapter(new DriveAdapter(drives, secondaryColor), (dialog, which) -> {
            Utils.dataPath = drives[which];
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit().putString(Consts.PATH_KEY, Utils.dataPath).apply();
            onDataPathReady();
        }).show();
    }

    private static @Nullable Locale getAppLocale() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) return null;
        return locales.get(0);
    }

    public static @NonNull Locale getLocale() {
        Locale locale = getAppLocale();
        if (locale == null) return Locale.getDefault();
        return locale;
    }

    public static String getAppLang() {
        return getLocale().getLanguage();
    }

    private void onDataPathReady() {
        new InitTask(this).start();
    }

    @SuppressLint("MissingPermission")
    private void onGainLocationPermission(boolean noSavedLocation) {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location location = null;
        if (providers.contains(LocationManager.GPS_PROVIDER))
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null && providers.contains(LocationManager.NETWORK_PROVIDER))
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) new LocationDecoder(location, this).start();
        else {
            if (providers.size() < 2) {
                if (noSavedLocation)
                    LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder().addLocationRequest(new LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 5000L).build()).build()).addOnCompleteListener(this, task -> {
                        if (!task.isSuccessful() && task.getException() instanceof ResolvableApiException)
                            try {
                                ((ResolvableApiException) task.getException()).startResolutionForResult(MainActivity.this, Codes.ENABLE_LOCATION_REQUEST);
                            } catch (Exception e) {
                                L.d(e);
                            }
                    });
            }
            providers = locationManager.getAllProviders();
            if (providers.contains(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 100, this);
            if (providers.contains(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 100, this);
        }
    }

    public void saveLocation(String name, int latitude, int longitude) {
        getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit()
                .putString(Consts.LOCATION_NAME, name)
                .putInt(Consts.LATITUDE, latitude)
                .putInt(Consts.LONGITUDE, longitude).apply();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof DirectionFragment)
            ((DirectionFragment) fragment).setLocation(latitude, longitude);
        if (fragment instanceof PrayerTimeFragment) ((PrayerTimeFragment) fragment).update();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        else if (item.getItemId() == R.id.setting)
            startActivity(new Intent(this, SettingActivity.class));
        else return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RecreateManager.needRecreate(RecreateManager.MAIN_ACTIVITY)) recreate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Codes.NOTIFICATION_PERMISSION_REQUEST:
                break;
            case Codes.LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    onGainLocationPermission(!getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).contains(Consts.LATITUDE));
                break;
            case Codes.STORAGE_PERMISSION_REQUEST:
                new MyAppUpdater(this).onRequestPermissionsResult(grantResults);
                break;
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        new LocationDecoder(location, this).start();
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    public void askReview() {
        ReviewManagerFactory.create(this).requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewManagerFactory.create(MainActivity.this).launchReviewFlow(MainActivity.this, task.getResult());
            } else startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(Consts.PLAY_LINK + BuildConfig.APPLICATION_ID)));
        });
    }

    public void onInitTaskFinish() {
        setFragment();
        new MyAppUpdater(this).checkForUpdate();
        checkNotificationPermission();
    }

    private void setFragment() {
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentById(R.id.container);
        if (fragment == null) {
            Fragment mainFragment = new MainFragment();
            mainFragment.setEnterTransition(new Slide(Gravity.END));
            mainFragment.setExitTransition(new Slide(Gravity.START));
            manager.beginTransaction().add(R.id.container, mainFragment).commitAllowingStateLoss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFragment();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                new AlertDialog.Builder(this).setCancelable(false)
                        .setMessage("Notification permission required")
                        .setPositiveButton("Give Permission", (dialog, which) -> requestNotificationPermission())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else requestNotificationPermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, Codes.NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    public void getLocation(boolean noSavedLocation) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            onGainLocationPermission(noSavedLocation);
        } else if (noSavedLocation) showLocationRequestDialog();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showLocationRequestDialog() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this).setCancelable(false)
                    .setTitle(R.string.permission_req)
                    .setMessage(R.string.loc_req)
                    .setPositiveButton("Give Permission", (dialog, which) -> requestLocationPermission())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else requestLocationPermission();
    }

    @SuppressLint("NewApi")
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Codes.LOCATION_PERMISSION_REQUEST);
    }

    public CharSequence getSuraName(int sura) {
        if (suraNames == null)
            suraNames = getResources().getStringArray(R.array.sura_transliteration);
        return suraNames[sura];
    }

    public boolean launchAlarmPermission() {
        if (cantScheduleAlarm()) {
            new AlertDialog.Builder(this).setTitle("Set alarm")
                    .setMessage("permission required to set alarm")
                    .setPositiveButton(R.string.ok, (dialog, which) -> launchAlarmRequest())
                    .show();
            return true;
        }
        return false;
    }

    private boolean cantScheduleAlarm() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !((AlarmManager) getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms();
    }

    public boolean launchFullscreenAlarm() {
        if (cantUseFullScreenIntent()) {
            new AlertDialog.Builder(this).setTitle("Set alarm")
                    .setMessage("permission required to set alarm")
                    .setPositiveButton(R.string.ok, (dialog, which) -> launchFullScreenRequest())
                    .show();
            return true;
        }
        return false;
    }

    private boolean cantUseFullScreenIntent() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).canUseFullScreenIntent();
    }

    private void launchAlarmRequest() {
        alarmPermissionLauncher.launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
    }

    private void launchFullScreenRequest() {
        fullscreenPermissionLauncher.launch(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
    }
}

