package bn.poro.quran.fragments.root;


import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.R;

class StickyHeader extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    private int headerHeight;
    public final View headerView;

    StickyHeader(RecyclerView recyclerView) {
        headerView = LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.word_group, recyclerView, false);
        recyclerView.post(() -> fixLayoutSize(recyclerView, headerView));
        ImageView imageView = headerView.findViewById(R.id.icon);
        imageView.setRotation(90);
    }

    @Override
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.State state) {
        super.onDrawOver(c, recyclerView, state);
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;
        int topChildPosition = layoutManager.findFirstVisibleItemPosition();
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter == null) return;
        if (adapter.getItemViewType(topChildPosition) == Consts.TYPE_GROUP
                && adapter.getItemViewType(topChildPosition + 1) == Consts.TYPE_GROUP) {
            headerHeight = 0;
            return;
        }
        View childInContact = getChildInContact(recyclerView);
        if (childInContact == null) return;
        int headerTop;
        if (adapter.getItemViewType(recyclerView.getChildAdapterPosition(childInContact)) == Consts.TYPE_GROUP) {
            headerHeight = childInContact.getTop();
            headerTop = headerHeight - headerView.getHeight();
        } else {
            headerTop = 0;
            headerHeight = headerView.getHeight();
        }
        c.save();
        c.translate(0, headerTop);
        headerView.draw(c);
        c.restore();
    }

    private View getChildInContact(RecyclerView parent) {
        int contactPoint = headerView.getBottom();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child.getBottom() > contactPoint) {
                if (child.getTop() <= contactPoint) {
                    return child;
                }
            }
        }
        return null;
    }

    private void fixLayoutSize(ViewGroup parent, View view) {
        /* int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY)
        //int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED);
        //int childWidthSpec = ViewGroup.getChildMeasureSpec(widthSpec, 0, view.getLayoutParams().width);
        //int childHeightSpec = ViewGroup.getChildMeasureSpec(heightSpec, 0, view.getLayoutParams().height);
         */
        view.measure(View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.UNSPECIFIED));
        headerHeight = view.getMeasuredHeight();
        view.layout(0, 0, view.getMeasuredWidth(), headerHeight);
    }

    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
        if (motionEvent.getY() > headerHeight || motionEvent.getAction() != MotionEvent.ACTION_DOWN)
            return false;
        WordAdapter adapter = (WordAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            recyclerView.scrollToPosition(adapter.expandedGroup);
            adapter.expand(adapter.expandedGroup, "");
        }
        return true;

    }

    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {
    }

    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}