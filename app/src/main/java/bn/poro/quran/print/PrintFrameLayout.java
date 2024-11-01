package bn.poro.quran.print;

import android.content.Context;
import android.widget.FrameLayout;

public class PrintFrameLayout extends FrameLayout {
    public PrintFrameLayout(Context activity) {
        super(activity);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightMeasureSpec) {
        int w = widthSpec & ~MeasureSpec.EXACTLY;
        int h = heightMeasureSpec & ~MeasureSpec.EXACTLY;
        setMeasuredDimension(w, h);
    }
}
