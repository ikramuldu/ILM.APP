package bn.poro.quran.pdf_section;

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
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;
import bn.poro.quran.activity_reader.PDFActivity;


public class PdfBookListFragment extends Fragment implements View.OnClickListener, ServiceConnection {

    private int latestId;
    private PdfNamesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) inflater.getContext();
        View view = inflater.inflate(R.layout.name_fragment, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        adapter = new PdfNamesAdapter(activity);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view == null) return;
        TextView textView = view.findViewById(R.id.latest);
        Context context = view.getContext();
        context.bindService(new Intent(context, DownloadService.class), this, Context.BIND_ABOVE_CLIENT);
        SharedPreferences store = context.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        latestId = store.getInt(Consts.LATEST_PDF, 0);
        if (latestId == 0) textView.setVisibility(View.GONE);
        else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(store.getString(Consts.LATEST_PDF_NAME, ""));
            textView.setOnClickListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unbindService(this);
    }

    @Override
    public void onClick(View v) {
        Context context = v.getContext();
        context.startActivity(new Intent(context, PDFActivity.class)
                .putExtra(Consts.ID_KEY, latestId)
                .putExtra(Consts.LATEST_PDF_NAME, ((TextView) v).getText().toString()));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DownloadService downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(adapter);
        adapter.setService(downloadService);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
