package bn.poro.quran.activity_quran;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.L;
import bn.poro.quran.Utils;

class ChangeTitleOnScrollListener extends RecyclerView.OnScrollListener {
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        QuranActivity activity = (QuranActivity) Utils.getContext(recyclerView);
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            activity.changeDraggingState(true);
        } else if (newState == RecyclerView.SCROLL_STATE_IDLE) try {
            if (activity.isAutoScrolling) {
                if (activity.isUserDragging()) {
                    activity.startScroll();
                    activity.changeDraggingState(false);
                } else activity.stopScroll();
            }
            AyahItem item = (AyahItem) activity.getAdapter().layoutManager.getChildAt(0).getTag();
            if (activity.currentSuraIndex == item.suraIndex) return;
            activity.currentSuraIndex = item.suraIndex;
            activity.changeTitle();
        } catch (Exception e) {
            L.d(e);
        }
    }
}
