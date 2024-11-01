package bn.poro.quran.activity_quran;

import static bn.poro.quran.activity_quran.SuraViewAdapter.SCROLL_LIMIT;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.R;

public class FullQuranPlayerListener implements QuranPlayerService.PlayerListener {
    private final QuranActivity activity;

    public FullQuranPlayerListener(QuranActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onPlay(int word, boolean press) {
        onPlay(activity.playingPosition, word, press);
    }

    @Override
    public void onPlay(int pos, int word, boolean press) {
        if (activity.withoutArabic()) return;
        FullQuranAdapter adapter = activity.getAdapter();
        if (adapter != null) adapter.notifyItemChanged(pos, new Object[]{word, press});
    }

    @Override
    public void onPlay(int sura, int ayah) {
        sura--;
        int position = QuranActivity.allSuraInfo.get(sura).suraStartIndex + ayah;
        FullQuranAdapter adapter = activity.getAdapter();
        if (adapter == null) return;
        if (activity.playingPosition != RecyclerView.NO_POSITION && activity.playingPosition != position) {
            adapter.notifyItemChanged(activity.playingPosition, new Object[]{SuraViewAdapter.HIGHLIGHT_WHOLE_AYAH, false});
        }
        if (activity.playingPosition != position) {
            activity.playingPosition = position;
            adapter.notifyItemChanged(position, new Object[]{SuraViewAdapter.HIGHLIGHT_WHOLE_AYAH, true});
        }
        if (!activity.scrollWithPlayer()) return;
        if (activity.currentSuraIndex != sura) {
            activity.currentSuraIndex = sura;
            activity.changeTitle();
        }
        int dif = position - adapter.layoutManager.findFirstVisibleItemPosition();
        if (dif > SCROLL_LIMIT || dif < -SCROLL_LIMIT)
            adapter.layoutManager.scrollToPositionWithOffset(position, 0);
        else {
            RecyclerView.SmoothScroller scroller = new LinearSmoothScroller(activity) {
                @Override
                protected int getVerticalSnapPreference() {
                    return SNAP_TO_ANY;
                }

                @Override
                protected int calculateTimeForScrolling(int dx) {
                    return QuranPlayerService.SCROLL_TIME;
                }
            };
            scroller.setTargetPosition(position);
            adapter.layoutManager.startSmoothScroll(scroller);
        }

    }

    @Override
    public void onPlay() {
        activity.hideScrollButton();
    }

    @Override
    public void onPause() {
        activity.showScrollButton();
  }

    @Override
    public void onStop() {
        if (activity.playingPosition != RecyclerView.NO_POSITION) {
            activity.playingPosition = RecyclerView.NO_POSITION;
            RecyclerView.Adapter<?> adapter = activity.getAdapter();
            if (adapter != null)
                adapter.notifyItemChanged(activity.playingPosition, new Object[]{SuraViewAdapter.HIGHLIGHT_WHOLE_AYAH, false});
        }
        activity.unbindService();
        activity.showScrollButton();
    }

    @Override
    public void onDownloadStart() {
        activity.playerController.findViewById(R.id.loading).setVisibility(View.VISIBLE);
        activity.playerController.findViewById(R.id.play).setVisibility(View.GONE);
    }

    @Override
    public void onMinuteElapse(int time) {
        ((TextView) activity.playerController.findViewById(R.id.stop_time)).setText(String.valueOf(time));

    }

    @Override
    public void onDownloadEnd() {
        activity.playerController.findViewById(R.id.loading).setVisibility(View.GONE);
        activity.playerController.findViewById(R.id.play).setVisibility(View.VISIBLE);
    }

}
