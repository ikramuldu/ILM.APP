package bn.poro.quran.fragments.sura_list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;

public class SuraListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list, container, false);
        MainActivity activity = (MainActivity) inflater.getContext();
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        recyclerView.setAdapter(new SuraNamesAdapter(activity));
        return recyclerView;
    }
}
