package bn.poro.quran.activity_quran;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

public class ScrollSpan extends ClickableSpan {
    private final QuranActivity activity;
    private final int sura;
    private final int ayah;

    public ScrollSpan(QuranActivity activity, int sura, int ayah) {
        this.activity = activity;
        this.sura = sura;
        this.ayah = ayah;
    }

    @Override
    public void onClick(@NonNull View widget) {
        activity.jumpTo(sura - 1, ayah);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setColor(paint.linkColor);
    }
}
