package bn.poro.quran.fragments.bookmark;

class ChildModel {
    final int id, sura, ayah;
    final String note;

    ChildModel(int sura, int ayah, String note, int id) {
        this.sura = sura;
        this.ayah = ayah;
        this.note = note;
        this.id = id;
    }
}
