package bn.poro.quran.media_section;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.DownloadService;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class IslamhouseFragment extends Fragment implements ServiceConnection {
    private DownloadService downloadService;
    private DownloadListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list, container, false);
        MediaHomeActivity activity = (MediaHomeActivity) inflater.getContext();
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        activity.bindService(new Intent(activity, DownloadService.class), this, Context.BIND_ABOVE_CLIENT);
        Bundle bundle = getArguments();
        assert bundle != null;
        adapter = new DownloadListAdapter(activity, bundle.getInt("p"));
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (downloadService != null) downloadService.setListener(adapter);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(adapter);
        adapter.setService(downloadService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
