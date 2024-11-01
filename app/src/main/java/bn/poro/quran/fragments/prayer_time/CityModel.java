package bn.poro.quran.fragments.prayer_time;

class CityModel {
    final String cityName;
    final int latitude, longitude;

    CityModel(String cityName, int latitude, int longitude) {
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
