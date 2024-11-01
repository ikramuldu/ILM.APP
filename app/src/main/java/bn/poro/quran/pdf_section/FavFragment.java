package bn.poro.quran.pdf_section;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;

public class FavFragment extends Fragment {
    public static boolean refreshRequired;
    private FavAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) inflater.getContext();
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list,
                container, false);
        LinearLayoutManager layoutManager = new MyLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FavAdapter(activity);
        recyclerView.setAdapter(adapter);
        refreshRequired = true;
        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (refreshRequired) {
            refreshRequired = false;
            adapter.refresh();
        }
    }
}
