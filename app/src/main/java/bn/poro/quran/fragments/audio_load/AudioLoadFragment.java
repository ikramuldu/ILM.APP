package bn.poro.quran.fragments.audio_load;

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

public class AudioLoadFragment extends Fragment {
    private AudioListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.toolbar_list, container, false);
        SettingActivity activity = (SettingActivity) inflater.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        adapter = new AudioListAdapter(activity);
        recyclerView.setAdapter(adapter);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
        activity.setTitle(R.string.pref_audio_manager);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.bindService();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.unbindService();
    }
}
