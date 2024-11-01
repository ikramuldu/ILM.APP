package bn.poro.quran.my_paging;

import android.util.SparseArray;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.L;

public class PagerDataModel<ITEM> {
    public static final int MAX_PAGE_TO_CACHE = 3;
    public static final int PAGE_SIZE = 5;
    private final RecyclerView.Adapter<?> adapter;
    private final LinearLayoutManager layoutManager;
    private final DataFetchManager<ITEM> dataFetchManager;
    private final SparseArray<DataSegment<ITEM>> availablePages;

    public PagerDataModel(RecyclerView.Adapter<?> adapter, LinearLayoutManager layoutManager, PagerDataProvider<ITEM> pagerDataProvider) {
        this.adapter = adapter;
        this.layoutManager = layoutManager;
        dataFetchManager = new DataFetchManager<>(pagerDataProvider, this);
        availablePages = new SparseArray<>(MAX_PAGE_TO_CACHE);
    }

    public ITEM getItem(int position) {
        int page = position / PAGE_SIZE;
        DataSegment<ITEM> segment = availablePages.get(page);
        if (segment != null) {
            return segment.get(position);
        } else {
            requirePage(page);
            return null;
        }
    }

    void requirePage(int page) {
        if (!dataFetchManager.requested(page)) {
            dataFetchManager.requestPage(page);
            L.d("requested: " + page);
        }
    }

    void onPageAvailable(DataSegment<ITEM> segment) {
        int page = segment.getPage();
        while (availablePages.size() >= MAX_PAGE_TO_CACHE) {
            int farthestPage = findFarthestFrom(page);
            availablePages.remove(farthestPage);
        }
        availablePages.put(page, segment);
        adapter.notifyItemRangeChanged(segment.getFrom(),
                segment.getTo() - segment.getFrom());
        L.d("Loaded: " + page);
    }

    private int findFarthestFrom(int page) {
        int maxDistance = 0;
        int result = 0;
        for (int index = 0; index < availablePages.size(); index++) {
            int p = availablePages.keyAt(index);
            int distance = Math.abs(page - p);
            if (distance > maxDistance) {
                maxDistance = distance;
                result = p;
            }
        }
        return result;
    }

    public boolean isPageStillRequired(int page) {
        int start = page * PAGE_SIZE;
        int end = (page + 1) * PAGE_SIZE;
        return end >= layoutManager.findFirstVisibleItemPosition() - 1 &&
                start <= layoutManager.findLastVisibleItemPosition() + 1;
    }
}
