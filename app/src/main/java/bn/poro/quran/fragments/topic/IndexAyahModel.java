package bn.poro.quran.fragments.topic;

class IndexAyahModel {
    final int sura, ayah;
    final String words;

    IndexAyahModel(int sura, int ayah, String words) {
        this.sura = sura;
        this.ayah = ayah;
        this.words = words;
    }
}
