package bn.poro.quran.views;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;

public class FontSpan extends TypefaceSpan {

    private final Typeface newType;

    public FontSpan(Typeface typeface) {
        super("");
        newType = typeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        applyCustomTypeFace(textPaint, newType);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        applyCustomTypeFace(textPaint, newType);
    }

    private static void applyCustomTypeFace(Paint paint, Typeface tf) {
        int oldStyle;
        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }
        int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }
        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }
        paint.setTypeface(tf);
    }
}