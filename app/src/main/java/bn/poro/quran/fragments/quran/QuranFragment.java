package bn.poro.quran.fragments.quran;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_search.SearchQuranActivity;

public class QuranFragment extends Fragment {
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.appbar_viewpager, container, false);
        MainActivity activity = (MainActivity) inflater.getContext();
        activity.setTitle(R.string.quran);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        view.post(this::init);
        return view;
    }

    private void init() {
        View view = getView();
        if (view == null) return;
        ViewPager2 viewPager = view.findViewById(R.id.view_pager2);
        PageAdapter adapter = new PageAdapter(this);
        viewPager.setAdapter(adapter);
        new TabLayoutMediator(view.findViewById(R.id.tabs), viewPager, adapter).attach();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        inflater.inflate(R.menu.setting, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.search) {
            startActivity(new Intent(getContext(), SearchQuranActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
