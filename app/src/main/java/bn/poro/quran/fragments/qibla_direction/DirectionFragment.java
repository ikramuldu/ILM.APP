package bn.poro.quran.fragments.qibla_direction;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.SENSOR_SERVICE;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;

public class DirectionFragment extends Fragment implements SensorEventListener {
    public static final float MULTIPLIER = 1000f;
    private static final float TO_RADIAN = (float) Math.PI / 180f;
    private static final float TO_DEGREE = 180f / (float) Math.PI;
    private ImageView imageView;
    float direction;
    private SensorManager sensorManager;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = mGravity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        assert activity != null;
        activity.setTitle(R.string.direction);
        sensorManager = (SensorManager) activity.getSystemService(SENSOR_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        imageView = (ImageView) inflater.inflate(R.layout.direction, container, false);
        return imageView;
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences prefs = requireContext().getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        ((MainActivity) imageView.getContext()).getLocation(!prefs.contains(Consts.LATITUDE));
        setLocation(prefs.getInt(Consts.LATITUDE, Consts.LATITUDE_DHAKA) / MULTIPLIER, prefs.getInt(Consts.LONGITUDE, Consts.LONGITUDE_DHAKA) / MULTIPLIER);
    }

    public void setLocation(float latitude, float longitude) {
        float dl = (Consts.TARGET_LONGITUDE - longitude) * TO_RADIAN;
        float l1 = latitude * TO_RADIAN;
        float l2 = Consts.TARGET_LATITUDE * TO_RADIAN;
        double y = Math.sin(dl) * Math.cos(l2);
        double x = Math.cos(l1) * Math.sin(l2) - Math.sin(l1) * Math.cos(l2) * Math.cos(dl);
        direction = ((float) Math.atan2(y, x)) * TO_DEGREE;
    }

    public void onResume() {
        super.onResume();
        if (failSensor(Sensor.TYPE_MAGNETIC_FIELD) || failSensor(Sensor.TYPE_ACCELEROMETER))
            Toast.makeText(getContext(), R.string.no_sensor, Toast.LENGTH_SHORT).show();
    }

    private boolean failSensor(int sensor) {
        return !sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(sensor), SensorManager.SENSOR_DELAY_UI);
    }

    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) mGravity = event.values;
        else mGeomagnetic = event.values;
        float[] R = new float[9];
        if (SensorManager.getRotationMatrix(R, null, mGravity, mGeomagnetic)) {
            imageView.setRotation(direction - (float) Math.atan2(R[1], R[4]) * TO_DEGREE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}

