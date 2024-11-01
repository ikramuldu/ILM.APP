package bn.poro.quran;

import android.content.Intent;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import bn.poro.quran.activity_quran.QuranActivity;

public class AyahClickSpan extends ClickableSpan {
    private final int sura, ayah;

   public AyahClickSpan(int sura, int ayah) {
        this.sura = sura;
        this.ayah = ayah;
    }

    @Override
    public void onClick(@NonNull View widget) {
        widget.getContext().startActivity(new Intent(widget.getContext(), QuranActivity.class)
                .putExtra(Consts.EXTRA_SURA_ID, sura)
                .putExtra(Consts.EXTRA_AYAH_NUM, ayah));
    }

    @Override
    public void updateDrawState(@NonNull TextPaint paint) {
        paint.setColor(paint.linkColor);
    }
}
