package bn.poro.quran.fragments.prayer_time;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import bn.poro.quran.L;
import bn.poro.quran.activity_main.MainActivity;

public class LocationDecoder extends Thread {
    private final MainActivity activity;
    private final Location location;

    public LocationDecoder(Location location, MainActivity activity) {
        this.location = location;
        this.activity = activity;
    }

    @Override
    public void run() {
        Geocoder geocoder = new Geocoder(activity);
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (Exception e) {
            L.d(e);
        }
        String address;
        if (null != addresses && !addresses.isEmpty())
            address = addresses.get(0).getAddressLine(0);
        else address = "Change Location";
        new Handler(Looper.getMainLooper()).post(() -> activity.saveLocation(address,
                (int) (location.getLatitude() * 1000),
                (int) (location.getLongitude() * 1000)));

    }
}
