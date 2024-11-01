package bn.poro.quran.app_updater;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class OpenableApp {
    private final ApplicationInfo applicationInfo;
    private final Intent openIntent;

    public OpenableApp(ApplicationInfo applicationInfo, Intent openIntent) {
        this.applicationInfo = applicationInfo;
        this.openIntent = openIntent;
    }

    public void open(Context context) {
        openIntent.setPackage(applicationInfo.packageName);
        context.startActivity(openIntent);
    }

    public CharSequence name(Context context) {
        return context.getPackageManager().getApplicationLabel(applicationInfo);
    }

    public Drawable icon(Context context) {
        Drawable drawable = applicationInfo.loadIcon(context.getPackageManager());
        int size = (int) (40 * context.getResources().getDisplayMetrics().density);
        drawable.setBounds(0, 0, size, size);
        return drawable;
    }
}
