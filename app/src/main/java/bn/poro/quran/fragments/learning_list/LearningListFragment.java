package bn.poro.quran.fragments.learning_list;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;

public class LearningListFragment extends Fragment {
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.setting, menu);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_list, container, false);
        MainActivity activity = (MainActivity) inflater.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        recyclerView.setAdapter(new LearningListAdapter());
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        activity.setTitle(R.string.learn_arabic);
        return view;
    }
}
