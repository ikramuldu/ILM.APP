package bn.poro.quran.pdf_section;

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

import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class PdfFragment extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.appbar_viewpager, container, false);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager2);
        PageAdapter adapter = new PageAdapter(this);
        viewPager.setAdapter(adapter);
        MainActivity activity = (MainActivity) inflater.getContext();
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        activity.setTitle(R.string.pdf_books);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        new TabLayoutMediator(view.findViewById(R.id.tabs), viewPager, adapter).attach();
        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.setting, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(getContext(), SettingActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
