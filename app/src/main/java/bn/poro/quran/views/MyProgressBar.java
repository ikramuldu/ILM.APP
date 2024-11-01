package bn.poro.quran.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.core.content.res.ResourcesCompat;

import bn.poro.quran.R;

public class MyProgressBar extends ProgressBar {
    public static final int NONE = 0;
    public static final int COMPLETED = 1;
    public static final int PARTIAL_DONE = 2;
    public static final int UPDATE = 3;
    public static final int WAITING = 4;
    public static final int DOWNLOADING = 5;
    public static final int CANCEL_ALL = 6;
    public int status;
    public static Paint paint;
    private static RectF dimn;
    private float progress;
    private static Drawable downloadIcon, circleIcon, cancelIcon, doneIcon, updateIcon;
    private Drawable thumb;

    public MyProgressBar(Context context) {
        this(context, null);
    }

    public MyProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    void init() {
        if (paint != null) return;
        Resources.Theme theme = getContext().getTheme();
        Resources resources = getResources();
        float density = resources.getDisplayMetrics().density;
        paint = new Paint();
        paint.setStrokeWidth(4 * density);
        paint.setAntiAlias(true);
        TypedArray typedArray = theme.obtainStyledAttributes(new int[]{R.attr.colorAccent});
        int accent = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else typedArray.recycle();
        paint.setColor(accent);
        paint.setStyle(Paint.Style.STROKE);
        downloadIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_download, theme);
        circleIcon = ResourcesCompat.getDrawable(resources, R.drawable.circular_shape, theme);
        cancelIcon = ResourcesCompat.getDrawable(resources, R.drawable.progress_bar_cancel, theme);
        doneIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete, theme);
        updateIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_db_update, theme);
        int w = (int) (50 * density);
        Rect rect = new Rect(0, 0, w, w);
        cancelIcon.setBounds(rect);
        circleIcon.setBounds(rect);
        int left = (int) (8 * density);
        int right = w - left;
        rect = new Rect(left, left, right, right);
        doneIcon.setBounds(rect);
        updateIcon.setBounds(rect);
        downloadIcon.setBounds(rect);
        left = (int) (6 * density);
        right = w - left;
        dimn = new RectF(left, left, right, right);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        switch (status) {
            case COMPLETED:
                if (thumb != null) thumb.draw(canvas);
                else doneIcon.draw(canvas);
                break;
            case UPDATE:
                updateIcon.draw(canvas);
                break;
            case CANCEL_ALL:
                cancelIcon.draw(canvas);
                break;
            case WAITING:
                super.onDraw(canvas);
                cancelIcon.draw(canvas);
                break;
            case DOWNLOADING:
                cancelIcon.draw(canvas);
                circleIcon.draw(canvas);
                canvas.drawArc(dimn, -90, progress, false, paint);
                break;
            case PARTIAL_DONE:
                circleIcon.draw(canvas);
                canvas.drawArc(dimn, -90, progress, false, paint);
            case NONE:
                downloadIcon.draw(canvas);
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setThumb(Drawable thumb) {
        if (thumb != null)
            thumb.setBounds(new Rect(0, 0, thumb.getIntrinsicWidth(), thumb.getIntrinsicHeight()));
        this.thumb = thumb;
    }
}
