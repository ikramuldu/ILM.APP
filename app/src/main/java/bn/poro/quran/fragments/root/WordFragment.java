package bn.poro.quran.fragments.root;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class WordFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list, container, false);
        Activity activity = (Activity) inflater.getContext();
        Bundle bundle = getArguments();
        if (bundle != null) activity.setTitle(bundle.getString("title"));
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        StickyHeader stickyHeader = new StickyHeader(recyclerView);
        recyclerView.setAdapter(new WordAdapter(activity, stickyHeader));
        recyclerView.addItemDecoration(stickyHeader);
        recyclerView.addOnItemTouchListener(stickyHeader);
        return recyclerView;
    }
}
