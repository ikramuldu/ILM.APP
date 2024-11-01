package bn.poro.quran.fragments.homepage;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;

public class MainFragment extends Fragment {
    private final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            View view = getView();
            if (view != null) {
                DrawerLayout drawerLayout = view.findViewById(R.id.drawer_layout);
                drawerLayout.close();
            }
        }
    };

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) inflater.getContext();
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        DrawerLayout drawerLayout = view.findViewById(R.id.drawer_layout);
        Toolbar toolbar = drawerLayout.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        activity.setTitle(R.string.app_name);
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        drawerList.setLayoutManager(new MyLayoutManager(activity));
        drawerList.setAdapter(new HomeDrawerAdapter(activity));
        activity.setSupportActionBar(toolbar);
        activity.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
        callback.setEnabled(false);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(activity, drawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                callback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                callback.setEnabled(false);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        RecyclerView recyclerView = drawerLayout.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, displayMetrics.widthPixels / (int) (150 * displayMetrics.density)));
        recyclerView.setAdapter(new HomeItemsAdapter(activity));
        return view;
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.setting, menu);
    }
}
