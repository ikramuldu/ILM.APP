package bn.poro.quran.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class WordGroup extends ViewGroup {
    private final ArrayList<WordLine> wordLines = new ArrayList<>();
    private boolean justified;

    public WordGroup(Context context) {
        super(context);
    }

    public WordGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WordGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        wordLines.clear();
        int viewWidth = widthSpec & ~MeasureSpec.EXACTLY;
        int maxWordHeight = 0;
        int lineTop = 0;
        int lineStartIndex = 0;
        int padding = getPaddingLeft() + getPaddingRight();
        int spareWidth = viewWidth - padding;
        for (int i = lineStartIndex; i < getChildCount(); i++) {
            View child = getChildAt(i);
            measureChild(child, MeasureSpec.makeMeasureSpec(widthSpec, MeasureSpec.AT_MOST), MeasureSpec.UNSPECIFIED);
            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();
            if (spareWidth < childWidth) {
                wordLines.add(new WordLine(lineTop, lineStartIndex, i - lineStartIndex, spareWidth));
                lineStartIndex = i;
                lineTop += maxWordHeight;
                spareWidth = viewWidth - padding;
                maxWordHeight = childHeight;
            } else if (childHeight > maxWordHeight) maxWordHeight = childHeight;
            spareWidth -= childWidth;
        }
        wordLines.add(new WordLine(lineTop, lineStartIndex, getChildCount() - lineStartIndex, 0));
        setMeasuredDimension(viewWidth, heightSpec + lineTop + maxWordHeight + getPaddingBottom() + getPaddingTop());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (WordLine wordLine : wordLines) {
            right = getMeasuredWidth() - getPaddingRight();
            for (int i = 0; i < wordLine.count; i++) {
                View child = getChildAt(wordLine.startIndex + i);
                bottom = wordLine.top + child.getMeasuredHeight();
                left = right - child.getMeasuredWidth();
                child.layout(left, wordLine.top, right, bottom);
                right = left;
                if (justified) {
                    right -= wordLine.wordGap;
                    if (i < wordLine.indivisibleGap) right--;
                }
            }
        }
    }

    public void setJustification(boolean justified) {
        this.justified = justified;
    }

    private static class WordLine {
        final int top;
        final int startIndex;
        final int count;
        int wordGap;
        int indivisibleGap;

        WordLine(int top, int startIndex, int count, int spareWidth) {
            this.top = top;
            this.startIndex = startIndex;
            this.count = count;
            if (count > 1) {
                count--;
                wordGap = spareWidth / count;
                indivisibleGap = spareWidth % count;
            }
        }
    }
}
