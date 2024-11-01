package bn.poro.quran.fragments.word_load;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class WordLoadFragment extends Fragment {
    private WordLangDownloadListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_list, container, false);
        SettingActivity activity = (SettingActivity) inflater.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null)
            bar.setDisplayHomeAsUpEnabled(true);
        activity.setTitle(R.string.word_by_word_language);
        adapter = new WordLangDownloadListAdapter(activity);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.unBindService();
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.bindService();
    }
}
