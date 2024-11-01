package bn.poro.quran.views;


import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import bn.poro.quran.L;

public class InstantCompleteView extends AppCompatAutoCompleteTextView {
    public InstantCompleteView(Context c) {
        super(c);
    }

    public InstantCompleteView(Context c, AttributeSet s) {
        super(c, s);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (focused) try {
            showDropDown();
        } catch (Exception e) {
            L.d(e);
        }
    }
}