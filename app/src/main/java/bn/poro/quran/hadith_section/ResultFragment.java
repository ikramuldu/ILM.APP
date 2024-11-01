package bn.poro.quran.hadith_section;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class ResultFragment extends Fragment implements Runnable {
    private SearchAdapter adapter;
    private LinearLayoutManager layoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list, container, false);
        SearchHadithActivity activity = (SearchHadithActivity) inflater.getContext();
        layoutManager = new MyLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SearchAdapter(activity, getArguments());
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        L.d("resume: " + getArguments());
        new Handler(Looper.getMainLooper()).post(this);
    }

    @Override
    public void run() {
        SearchHadithActivity activity = (SearchHadithActivity) getActivity();
        if (activity != null)
            activity.drawerAdapter.update(layoutManager, adapter.getCursor());
    }
}
