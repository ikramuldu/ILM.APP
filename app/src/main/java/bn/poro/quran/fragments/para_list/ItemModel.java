package bn.poro.quran.fragments.para_list;

class ItemModel {
    final int sura, ayah;
    final String text;

    ItemModel(int sura, int ayah, String text) {
        this.sura = sura;
        this.ayah = ayah;
        this.text = text;
    }
}
