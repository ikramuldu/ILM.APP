package bn.poro.quran.fragments.alphabet;

import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.R;

public class AlphabetFragment extends Fragment {
    static int spanCount;
    private AlphabetAdapter adapter;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AppCompatActivity activity = (AppCompatActivity) inflater.getContext();
        View view = inflater.inflate(R.layout.toolbar_list, container, false);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        Bundle bundle = getArguments();
        if (bundle != null) {
            activity.setTitle(bundle.getString("title"));
            adapter = new AlphabetAdapter(activity, bundle.getString("table"));
            recyclerView.setAdapter(adapter);
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(activity, spanCount);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position == 0) return spanCount;
                return 1;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.download) adapter.download();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        SharedPreferences preferences = requireContext().getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        if (!preferences.contains(Consts.LEARNING_AUDIO_DOWNLOADED) || preferences.getBoolean(Consts.LEARNING_AUDIO_DOWNLOADED, false))
            menu.removeItem((R.id.download));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.download, menu);
        inflater.inflate(R.menu.setting, menu);
    }
}
