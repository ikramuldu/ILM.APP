package bn.poro.quran.hadith_section;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;

public class FabFragment extends Fragment {

    private BookmarkAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Context context = inflater.getContext();
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list,
                container, false);
        adapter = new BookmarkAdapter(context);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager linearLayoutManager = new MyLayoutManager(context);
        recyclerView.setLayoutManager(linearLayoutManager);
        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.refresh();
        adapter.notifyDataSetChanged();
    }
}
