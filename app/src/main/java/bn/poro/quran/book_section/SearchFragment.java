package bn.poro.quran.book_section;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

public class SearchFragment extends Fragment implements View.OnClickListener, Runnable {
    private int currentResultId, currentPositionInPairs;
    private SearchResult searchResult;

    @Override
    public void onHiddenChanged(boolean hidden) {
        L.d("onHiddenChanged " + searchResult);
        super.onHiddenChanged(hidden);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        L.d("create search fragment");
        View view = inflater.inflate(R.layout.book_search_fragment, container, false);
        Activity activity = (Activity) Utils.getContext(view);
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        LinearLayoutManager layoutManager = new MyLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        assert getArguments() != null;
        searchResult = new SearchResult(getArguments());
        MainTextAdapter adapter = new MainTextAdapter(activity, layoutManager, searchResult.id);
        recyclerView.setAdapter(adapter);
        view.findViewById(R.id.go).setOnClickListener(this);
        TextView textView = view.findViewById(R.id.text);
        currentResultId = 1;
        currentPositionInPairs = 0;
        if (searchResult.total == SearchResult.NAME_MATCH) {
            textView.setText(R.string.name_match);
            view.findViewById(R.id.up).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.down).setVisibility(View.INVISIBLE);
        } else if (searchResult.total == SearchResult.NO_MATCH) {
            textView.setText(R.string.not_found);
            view.findViewById(R.id.up).setVisibility(View.INVISIBLE);
            view.findViewById(R.id.down).setVisibility(View.INVISIBLE);
        } else {
            view.findViewById(R.id.up).setOnClickListener(this);
            view.findViewById(R.id.down).setOnClickListener(this);
            textView.setText(getString(R.string.show_res, currentResultId, searchResult.total));
            adapter.resultAtPos = searchResult.ids[currentPositionInPairs];
            recyclerView.post(this);
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.go) {
            startActivity(new Intent(v.getContext(), ReadBookActivity.class)
                    .putExtra(Consts.ID_KEY, searchResult.id)
                    .putExtra(Consts.TITLE_KEY, searchResult.bookName));
            return;
        }
        View view = (View) v.getParent();
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        TextView textView = view.findViewById(R.id.text);
        MainTextAdapter adapter = (MainTextAdapter) recyclerView.getAdapter();
        assert adapter != null;
        Object payload = new Object();
        AppBarLayout appBarLayout = view.getRootView().findViewById(R.id.appbar);
        appBarLayout.setExpanded(false);
        if (v.getId() == R.id.up) {
            adapter.resultOffset--;
            currentResultId--;
            adapter.notifyItemChanged(adapter.resultAtPos, payload);
            if (adapter.resultOffset < 0) {
                if (currentResultId == 0) {
                    currentPositionInPairs = searchResult.ids.length - 1;
                    currentResultId = searchResult.total;
                } else currentPositionInPairs--;
                adapter.resultAtPos = searchResult.ids[currentPositionInPairs];
                adapter.resultOffset = searchResult.counts[currentPositionInPairs] - 1;
                adapter.notifyItemChanged(adapter.resultAtPos, payload);
            }
        } else {
            adapter.resultOffset++;
            currentResultId++;
            adapter.notifyItemChanged(adapter.resultAtPos, payload);
            if (adapter.resultOffset >= searchResult.counts[currentPositionInPairs]) {
                if (currentResultId > searchResult.total) {
                    currentPositionInPairs = 0;
                    currentResultId = 1;
                } else currentPositionInPairs++;
                adapter.resultAtPos = searchResult.ids[currentPositionInPairs];
                adapter.resultOffset = 0;
                adapter.notifyItemChanged(adapter.resultAtPos, payload);
            }
        }
        int top = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(0));
        int bottom = recyclerView.getChildLayoutPosition(recyclerView.getChildAt(recyclerView.getChildCount() - 1));
        if (adapter.resultAtPos < top || adapter.resultAtPos > bottom) {
            adapter.layoutManager.scrollToPositionWithOffset(adapter.resultAtPos, 0);
        }
        textView.setText(getString(R.string.show_res, currentResultId, searchResult.total));
    }

    @Override
    public void run() {
        View view = getView();
        if (view == null) return;
        RecyclerView recyclerView = view.findViewById(R.id.main_list);
        recyclerView.scrollToPosition(searchResult.ids[currentPositionInPairs]);
    }
}
