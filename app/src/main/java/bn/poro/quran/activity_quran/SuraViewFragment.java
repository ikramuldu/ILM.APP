package bn.poro.quran.activity_quran;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

public class SuraViewFragment extends Fragment {

    SuraViewAdapter adapter;
    int suraIndex;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.sura_fragment, container, false);
        QuranActivity activity = (QuranActivity) inflater.getContext();
        LinearLayoutManager layoutManager = new MyLayoutManager(activity);
        if (getArguments() != null)
            suraIndex = getArguments().getInt(Consts.EXTRA_SURA_ID, 0);
        adapter = new SuraViewAdapter(activity, layoutManager, suraIndex);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addOnScrollListener(new MyScrollListener());
        return recyclerView;
    }

    private static class MyScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            QuranActivity activity = (QuranActivity) Utils.getContext(recyclerView);
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                activity.changeDraggingState(true);
            } else if (newState == RecyclerView.SCROLL_STATE_IDLE && activity.isAutoScrolling) {
                if (activity.isUserDragging()) {
                    activity.startScroll();
                    activity.changeDraggingState(false);
                } else {
                    activity.stopScroll();
                }
            }
        }
    }

}
