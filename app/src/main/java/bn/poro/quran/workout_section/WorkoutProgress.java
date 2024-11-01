package bn.poro.quran.workout_section;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import bn.poro.quran.R;

public class WorkoutProgress extends View {
    private final Paint paint;
    private final RectF dimn;
    private final float density;
    private float progress;
    int text;
    private final Drawable circleIcon;
    final TextPaint textPaint;

    public WorkoutProgress(Context context) {
        this(context, null);
    }

    public WorkoutProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WorkoutProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Resources resources = context.getResources();
        density = resources.getDisplayMetrics().density;
        paint = new Paint();
        paint.setStrokeWidth(4 * density);
        paint.setAntiAlias(true);
        paint.setTextSize(25 * density);
        paint.setColor(0xbb25b7d3);
        paint.setStyle(Paint.Style.STROKE);
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(0xbb25b7d3);
        textPaint.setTextSize(70 * density);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        circleIcon = ResourcesCompat.getDrawable(resources, R.drawable.circular_shape,context.getTheme());
        assert circleIcon != null;
        circleIcon.setBounds(0, 0, (int) (250 * density), (int) (250 * density));
        dimn = new RectF(40 * density, 40 * density, 210 * density, 210 * density);
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    protected synchronized void onDraw(@NonNull Canvas canvas) {
        circleIcon.draw(canvas);
        canvas.drawArc(dimn, -90, progress, false, paint);
        String s=String.valueOf(text);
        canvas.drawText(s, (250*density-textPaint.measureText(s))/2, 145*density, textPaint);
    }
}

