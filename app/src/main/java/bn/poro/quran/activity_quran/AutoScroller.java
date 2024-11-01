package bn.poro.quran.activity_quran;

import android.app.Activity;

import androidx.recyclerview.widget.LinearSmoothScroller;

public class AutoScroller extends LinearSmoothScroller {
    private final float timePerPixel;

    public AutoScroller(Activity activity, float time) {
        super(activity);
        this.timePerPixel = time;
    }

    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_END;
    }

    @Override
    protected int calculateTimeForScrolling(int dx) {
        return (int) (Math.abs(dx) * timePerPixel);
    }
}
