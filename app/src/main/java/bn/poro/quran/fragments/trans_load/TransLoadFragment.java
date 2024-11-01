package bn.poro.quran.fragments.trans_load;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class TransLoadFragment extends Fragment {

    private TransDownloadListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        SettingActivity activity = (SettingActivity) inflater.getContext();
        View view = getLayoutInflater().inflate(R.layout.trans_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        Spinner spinner = view.findViewById(R.id.spinner);
        spinner.setAdapter(new MySpinnerAdapter());
        adapter = new TransDownloadListAdapter(activity);
        spinner.setOnItemSelectedListener(adapter);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        activity.setTitle(R.string.choose_translations);
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
