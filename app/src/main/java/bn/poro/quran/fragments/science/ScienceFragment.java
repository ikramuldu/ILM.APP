package bn.poro.quran.fragments.science;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;

public class ScienceFragment extends Fragment {
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        inflater.inflate(R.menu.setting, menu);
    }

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_list,
                container, false);
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        MainActivity activity = (MainActivity) inflater.getContext();
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        LinearLayoutManager layoutManager = new MyLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        
        recyclerView.setAdapter(new ScienceAdapter(activity, layoutManager));
        activity.setTitle(R.string.science);
        return view;
    }
}
