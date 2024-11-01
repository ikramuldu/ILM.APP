package bn.poro.quran.fragments.prayer_time;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import bn.poro.quran.fragments.qibla_direction.DirectionFragment;

class Calculator {
    private final float year100, conv1, conv2, latitude, longitude;
    private final int day, month, year, convention;

    Calculator(int latitude, int longitude, Date date, int convention) {
        this.latitude = latitude / DirectionFragment.MULTIPLIER;
        this.longitude = longitude / DirectionFragment.MULTIPLIER;
        this.year = date.getYear() + 1900;
        this.month = date.getMonth() + 1;
        this.day = date.getDate();
        this.convention = convention;
        year100 = (m1116a() - 2451545) / 36525.0f;
        if (convention == 0) {
            conv1 = 18.0f;
            conv2 = 17.0f;
        } else if (convention == 1) {
            conv1 = 15.0f;
            conv2 = 15.0f;
        } else if (convention == 2) {
            conv1 = 19.5f;
            conv2 = 17.5f;
        } else if (convention == 3) {
            conv1 = 18.5f;
            conv2 = 0.83333333f;
        } else {
            conv1 = 18.0f;
            conv2 = 18.0f;
        }
    }

    private double m1123b(double d) {
        double radians = Math.toRadians(m1125c(d));
        double radians2 = Math.toRadians(m1127e(d));
        double radians3 = Math.toRadians(((35999.05029 - (1.537E-4 * d)) * d) + 357.52911);
        double d2 = 0.016708634 - (((1.267E-7 * d) + 4.2037E-5) * d);
        double tan = Math.tan(radians / 2.0);
        double d3 = tan * tan;
        double d4 = radians2 * 2.0;
        double sin = Math.sin(d4);
        double cos = Math.cos(d4);
        double sin2 = Math.sin(radians2 * 4.0);
        double sin3 = Math.sin(radians3);
        return Math.toDegrees(((((((d2 * 4.0) * d3) * sin3) * cos) + ((sin * d3) - ((2.0 * d2) * sin3)))
                - (((0.5 * d3) * d3) * sin2)) - (((1.25 * d2) * d2) * Math.sin(radians3 * 2.0d))) * 4.0d;
    }

    private double m1125c(double d) {
        return (Math.cos(Math.toRadians(125.04d - (d * 1934.136d))) * 0.00256d) +
                ((((21.448d - ((((5.9E-4d - (0.001813d * d)) * d) + 46.815d) * d)) / 60.0d) + 26.0d) / 60.0d) + 23.0d;
    }

    private double m1126d(double d) {
        double radians = Math.toRadians(m1125c(d));
        double radians2 = Math.toRadians(125.04d - (1934.136d * d));
        double e = m1127e(d);
        double radians3 = Math.toRadians(((35999.05029d - (1.537E-4d * d)) * d) + 357.52911d);
        double sin = Math.sin(radians3 * 3.0d) * 2.89E-4d;
        double radians4 = Math.toRadians((((sin + (((0.019993d - (1.01E-4d * d))
                * Math.sin(2.0d * radians3)) + ((1.914602d - (((1.4E-5d * d) + 0.004817d) * d))
                * Math.sin(radians3)))) + e) - 0.00569d) - (Math.sin(radians2) * 0.00478d));
        return Math.toDegrees(Math.asin(Math.sin(radians4) * Math.sin(radians)));
    }

    private double m1127e(double d) {
        double d2 = (((3.032E-4d * d) + 36000.76983d) * d) + 280.46646d;
        return d2 - (Math.floor(d2 / 360.0d) * 360.0d);
    }

    private double mo1704a(double d, double d2) {
        double d1 = Math.acos(((-Math.sin(Math.toRadians(d)))
                - (Math.sin(Math.toRadians(m1126d(d2))) * Math.sin(Math.toRadians(latitude))))
                / (Math.cos(Math.toRadians(m1126d(d2)))
                * Math.cos(Math.toRadians(latitude))));
        return Double.isNaN(d1) ? d1 : Math.toDegrees(d1) * 0.06666666666666667d;
    }

    private double mo1710c() {
        return (12.0d - (longitude / 15.0d)) - (m1123b(year100) / 60.0d);
    }

    private double mo1711d() {
        return (12.0d - (longitude / 15.0d)) - (m1123b(year100 + 2.7378507871321012E-5d) / 60.0d);
    }

    private double mo1713f() {
        return (12.0d - (longitude / 15.0d)) - (m1123b(year100 - 2.7378507871321012E-5d) / 60.0d);
    }

    private long mo1716i(long magribTime) {
        double a = mo1704a(conv1, year100 + 2.7378507871321012E-5d);
        if (Double.isNaN(a)) {
            double d2 = 60.0d / conv1;
            long q3 = mo1724q();
            double q4 = (double) (mo1724q() - magribTime);
            return (q3 - ((long) (q4 / d2)));
        }
        long a2 = m1117a((mo1711d() - a)) + 86400000;
        return nextMinute(a2);
    }

    private long mo1724q() {
        long a = m1117a((mo1711d() - mo1704a(0.8333333333333334d,
                year100 + 2.7378507871321012E-5d))) + 86400000;
        return nextMinute(a);
    }

    private long mo1720m() {
        return m1117a((mo1704a(0.8333333333333334d,
                year100 - 2.7378507871321012E-5d) + mo1713f())) - 86400000;
    }

    long fazrTime(long sunrise) {
        double a = mo1704a(conv1, year100);
        if (Double.isNaN(a)) {
            double d2 = 60.0d / conv1;
            double r4 = (double) (sunrise - mo1720m());
            return (sunrise - ((long) (r4 / d2)));
        }
        long a2 = m1117a((mo1710c() - a));
        return nextMinute(a2);
    }

    long juhrTime() {
        long a = m1117a(mo1710c());
        return nextMinute(a);
    }

    /**
     * @param school hanafi-2, other-1
     */
    long asrTime(int school) {
        long a = m1117a(mo1710c()
                + mo1704a(Math.toDegrees(Math.atan(1.0d / (Math.tan(Math.toRadians(Math.abs(latitude
                - m1126d(year100)))) + school))) * -1.0d, year100));
        return nextMinute(a);
    }

    public long ishaTime(long magribTime) {
        double a = mo1704a(conv2, year100);
        double d2 = convention == 3 ? m1120b(m1116a())[1] == 9 ? 2.0 : 1.5 : 0;
        if (Double.isNaN(a)) {
            double d3 = 60.0d / conv2;
            double q2 = (double) (mo1724q() - magribTime);
            return magribTime + ((long) (q2 / d3)) + ((long) ((d2) * 3600.0d * 1000.0d));
        }
        long a2 = m1117a(mo1710c() + a + d2);
        return nextMinute(a2);
    }

    public long lastNightTime(long magribTime) {
        long a = (((mo1716i(magribTime) - magribTime) / 3) * 2) + magribTime;
        return nextMinute(a);
    }

    long magribTime() {
        return m1117a(mo1704a(0.8333333333333334d, year100) + mo1710c());
    }

    long sunriseTime() {
        return m1117a((mo1710c() - mo1704a(0.8333333333333334d, year100)));
    }

    static long prevMinute(long a) {
        return (a - (a % MINUTE_IN_MILLIS));
    }

    static long nextMinute(long a) {
        return (a - (a % MINUTE_IN_MILLIS)) + MINUTE_IN_MILLIS;
    }

    public long midnight(long magrib) {
        long a = ((mo1716i(magrib) - magrib) / 2) + magrib;
        return prevMinute(a);
    }

    long m1116a() {
        long j = year;
        if (month < 3) {
            j--;
        }
        long j2 = month < 3 ? ((long) month) + 12 : (long) month;
        long j3 = j / 100;
        long j4 = 2 - j3;
        double d = (double) (j + 4716);
        double d2 = (double) (j2 + 1);
        return (((((long) (d * 365.25)) + ((long) (d2 * 30.6001))) + ((long) day)) + ((j3 / 4) + j4)) - 1524;
    }

    long m1117a(double d) {
        int i4 = (int) d;
        double d3 = d - (double) i4;
        int i5 = (int) (d3 * 60.0);
        int i6 = (int) ((d3 - ((double) i5 / 60.0)) * 3600.0);
        try {
            return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.US)
                    .parse(String.format(Locale.US, "%04d/%02d/%02d %02d:%02d:%02d +0000",
                            year, month, day, i4, i5, i6)).getTime();
        } catch (Exception unused) {
            return 0;
        }
    }

    int[] m1120b(long j) {
        long j2 = ((j * 30) - 58442554) / 10631;
        long j3 = j - (((10631 * j2) + 58442583) / 30);
        long j4 = ((j3 * 11) + 330) / 325;
        long j5 = 1;
        int i = 0;
        long j6 = (j3 - (((325 * j4) - 320) / 11)) + 1 + ((long) i);
        if (j6 <= 0) {
            j6 = i + 31;
            j4--;
            if (j4 <= 0) {
                j2--;
                j4 = 1;
            }
        }
        if (j6 > 30) {
            j6 = 0;
            j4++;
            if (j4 > 12) {
                j2++;
                return new int[]{(int) j2, (int) j5, (int) j6};
            }
        }
        j5 = j4;
        return new int[]{(int) j2, (int) j5, (int) j6};
    }

    public long startOf(int position) {
        switch (position) {
            case 0:
                return fazrTime(sunriseTime());
            case 1:
                return sunriseTime();
            case 2:
                return sunriseTime() + PrayerTimeFragment.SUNRISE_SUNSET_TIME + MINUTE_IN_MILLIS;
            case 3:
                return juhrTime() - PrayerTimeFragment.MIDDAY_CAUTION_TIME;
            case 4:
                return juhrTime();
            case 5:
                return asrTime(2);
            case 6:
                return nextMinute(magribTime()) - MINUTE_IN_MILLIS - PrayerTimeFragment.SUNRISE_SUNSET_TIME;
            case 7:
                return nextMinute(magribTime()) + PrayerTimeFragment.MAGRIB_CAUTION_TIME;
            case 8:
                return ishaTime(magribTime());
            default:
                return lastNightTime(magribTime());
        }
    }
}
