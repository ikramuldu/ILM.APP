package bn.poro.quran.fragments.noun;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class NounFragment extends Fragment {
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search,menu);
        inflater.inflate(R.menu.setting, menu);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.toolbar_list, container, false);
        AppCompatActivity activity = (AppCompatActivity) inflater.getContext();
        Bundle bundle = getArguments();
        RecyclerView recyclerView=view.findViewById(R.id.main_list);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        activity.setTitle(bundle.getString("title"));
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        recyclerView.setAdapter(new NounAdapter(activity,bundle.getString("table")));
        return view;
    }
}
