package bn.poro.quran;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class L {

    public static void d(Exception e) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("my-tag", e.getClass() + ": " + e.getMessage());
            e.printStackTrace();
        } else FirebaseCrashlytics.getInstance().recordException(e);
    }

    public static void d(String s) {
        if (BuildConfig.DEBUG)
            android.util.Log.d("my-tag", s);
    }

    public static void e(String s) {
        if (!BuildConfig.DEBUG) FirebaseCrashlytics.getInstance().log(s);
    }
}
