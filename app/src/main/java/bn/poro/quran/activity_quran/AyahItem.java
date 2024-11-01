package bn.poro.quran.activity_quran;

public class AyahItem {
    final int ayah;
    final int juz;
    String[] translations;
    final int suraIndex;
    final String arabicText;
    String[][] words;
    final int wordStart;

    public AyahItem(int suraIndex, int ayah, String arabicText, int juz, int wordStart) {
        this.arabicText = arabicText;
        this.suraIndex = suraIndex;
        this.ayah = ayah;
        this.juz = juz;
        this.wordStart = wordStart;
    }
}
