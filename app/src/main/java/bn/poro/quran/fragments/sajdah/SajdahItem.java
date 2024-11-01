package bn.poro.quran.fragments.sajdah;

import android.text.SpannableStringBuilder;

class SajdahItem {
    final int sura, ayah;
    final CharSequence text;

    SajdahItem(int sura, int ayah, SpannableStringBuilder text) {
        this.sura = sura;
        this.ayah = ayah;
        this.text = text;
    }
}
