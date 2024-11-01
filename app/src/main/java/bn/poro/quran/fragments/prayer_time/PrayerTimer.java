package bn.poro.quran.fragments.prayer_time;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static android.text.format.DateUtils.YEAR_IN_MILLIS;

import android.os.CountDownTimer;

class PrayerTimer extends CountDownTimer {
    private final PrayerTimeFragment fragment;

    PrayerTimer(PrayerTimeFragment fragment) {
        super(YEAR_IN_MILLIS, SECOND_IN_MILLIS);
        this.fragment = fragment;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        fragment.updateTime();
    }

    @Override
    public void onFinish() {

    }
}
