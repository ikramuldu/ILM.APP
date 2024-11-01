package bn.poro.quran.book_section;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.DownloadService;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.Consts;

public class NamesFragment extends Fragment implements View.OnClickListener {

    private TextView textView;
    private BookNamesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.name_fragment, container, false);
        LinearLayoutManager linearLayoutManager = new MyLayoutManager(inflater.getContext());
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        textView = view.findViewById(R.id.latest);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new BookNamesAdapter(view.getContext(), linearLayoutManager);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = textView.getContext();
        context.bindService(new Intent(context, DownloadService.class), adapter, Context.BIND_ABOVE_CLIENT);
        SharedPreferences store = context.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        String id = store.getString(Consts.LATEST_BOOK_ID, null);
        if (id != null) {
            textView.setVisibility(View.VISIBLE);
            textView.setTag(id);
            textView.setText("সর্বশেষঃ " + store.getString(Consts.LATEST_BOOK_TITLE, ""));
            textView.setOnClickListener(this);
        } else textView.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        textView.getContext().unbindService(adapter);
    }

    @Override
    public void onClick(@NonNull View view) {
        Context context = view.getContext();
        context.startActivity(new Intent(context, ReadBookActivity.class)
                .putExtra(Consts.ID_KEY, (String) view.getTag())
                .putExtra(Consts.TITLE_KEY, textView.getText().toString().substring(9)));
    }
}
