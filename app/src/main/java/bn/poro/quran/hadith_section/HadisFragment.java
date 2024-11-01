package bn.poro.quran.hadith_section;

import static android.content.Context.MODE_PRIVATE;

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
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;

public class HadisFragment extends Fragment {
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (Utils.dataPath == null) {
            Utils.dataPath = requireContext().getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE)
                    .getString(Consts.PATH_KEY, null);
        }
        new CheckHadisDatabasesTask().start();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
        inflater.inflate(R.menu.setting, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        L.d(item.toString());
        if (item.getItemId() == R.id.search) {
            startActivity(new Intent(getContext(), SearchHadithActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.appbar_viewpager, container, false);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager2);
        HadisPagerAdapter adapter = new HadisPagerAdapter(this);
        viewPager.setAdapter(adapter);
        MainActivity activity = (MainActivity) inflater.getContext();
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        activity.setTitle(R.string.hadis);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        new TabLayoutMediator(view.findViewById(R.id.tabs), viewPager, adapter).attach();
        return view;
    }
}
