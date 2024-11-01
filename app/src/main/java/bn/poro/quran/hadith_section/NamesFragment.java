package bn.poro.quran.hadith_section;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class NamesFragment extends Fragment implements View.OnClickListener, ServiceConnection {

    private NamesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.name_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        adapter = new NamesAdapter(context);
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.latest).setOnClickListener(this);
        LinearLayoutManager linearLayoutManager = new MyLayoutManager(context);
        recyclerView.setLayoutManager(linearLayoutManager);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view == null) return;
        Context context = view.getContext();
        context.bindService(new Intent(context, DownloadService.class), this, Context.BIND_ABOVE_CLIENT);
        SharedPreferences preferences = context.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        TextView textView = view.findViewById(R.id.latest);
        if (preferences.contains(Consts.LATEST_HADIS_BOOK)) {
            textView.setVisibility(View.VISIBLE);
            textView.setTag(preferences.getString(Consts.LATEST_HADIS, null));
            textView.setText(context.getString(R.string.bookmark_last_read) + preferences.getString(Consts.LATEST_HADIS_BOOK, null));
        } else textView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        Context context = getContext();
        if (context != null)
            context.startActivity(new Intent(context, ReadHadisActivity.class)
                    .putExtra(Consts.ID_KEY, (String) v.getTag()));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DownloadService downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(adapter);
        adapter.setService(downloadService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        adapter.setService(null);
    }
}
