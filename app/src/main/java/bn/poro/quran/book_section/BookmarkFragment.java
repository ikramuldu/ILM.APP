package bn.poro.quran.book_section;

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

public class BookmarkFragment extends Fragment {
    private BookmarkAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(R.layout.fast_scroll_list, container, false);
        LinearLayoutManager linearLayoutManager = new MyLayoutManager(inflater.getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new BookmarkAdapter(inflater.getContext());
        recyclerView.setAdapter(adapter);
        return recyclerView;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.refresh();
    }
}
