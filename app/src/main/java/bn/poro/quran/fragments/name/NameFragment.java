package bn.poro.quran.fragments.name;

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

public class NameFragment extends Fragment {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_list,
                container, false);
        MainActivity activity= (MainActivity) inflater.getContext();
        LinearLayoutManager layoutManager=new MyLayoutManager(activity);
        RecyclerView recyclerView=view.findViewById(R.id.main_list);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(new NamesAdapter(activity,layoutManager));
        activity.setTitle(R.string.names);
        return view;
    }
}
