package bn.poro.quran.fragments.prayer_time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bn.poro.quran.L;
import bn.poro.quran.R;

public class MyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        L.d(action);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            AlarmService.resetAlarms(context.getApplicationContext(), false);
        } else try {
            context.startService(new Intent(context, AlarmService.class).setAction(action));
        } catch (Exception e) {
            L.d(e);
            L.e("MyReceiver.onReceive: "+action);
            //https://stackoverflow.com/questions/51452301/java-lang-illegalstateexception-at-android-app-contextimpl-startservicecommon
        }
    }

    public int getStandardName(int i) {
        if (i == 0) {
            return R.string.convention_name_mwl;
        }
        if (i == 1) {
            return R.string.convention_name_isna;
        }
        if (i == 2) {
            return R.string.convention_name_egypt;
        }
        if (i == 3) {
            return R.string.convention_name_makkah;
        }
        if (i != 4) {
            return -1;
        }
        return R.string.convention_name_karachi;
    }
}
