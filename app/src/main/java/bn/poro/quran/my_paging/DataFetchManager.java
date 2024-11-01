package bn.poro.quran.my_paging;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import bn.poro.quran.L;

class DataFetchManager<ITEM> {
    private final PagerDataModel<ITEM> dataModel;
    private final ExecutorService executor;
    private boolean checkingQueue;
    private final ArrayList<Integer> requestedPages;
    private final PagerDataProvider<ITEM> dataProvider;

    DataFetchManager(PagerDataProvider<ITEM> dataProvider, PagerDataModel<ITEM> dataModel) {
        this.dataProvider = dataProvider;
        this.dataModel = dataModel;
        executor = new ThreadPoolExecutor(0, 1, 2L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        executor.execute(this.dataProvider::init);
        requestedPages = new ArrayList<>();
    }

    public void requestPage(final int page) {
        requestedPages.add(page);
        if (!checkingQueue) {
            checkingQueue = true;
            new Handler(Looper.getMainLooper()).post(this::checkQueue);
        }
    }

    private void checkQueue() {
        for (int index = requestedPages.size() - 1; index >= 0; index--) {
            int page = requestedPages.get(index);
            if (dataModel.isPageStillRequired(page)) {
                executor.execute(new Task(page));
                return;
            } else requestedPages.remove(index);
        }
        checkingQueue = false;
    }

    public boolean requested(int page) {
        return requestedPages.contains(page);
    }

    public void dataAvailable(int request, List<ITEM> response) {
        requestedPages.remove((Integer) request);
        DataSegment<ITEM> segment = new DataSegment<>(dataModel, request);
        segment.setData(response);
        checkQueue();
    }

    private class Task implements Runnable {
        private final int page;
        private List<ITEM> data;

        public Task(int page) {
            this.page = page;
        }

        @Override
        public void run() {
            L.d("running task: "+page);
            data = dataProvider.fetch(page);
            new Handler(Looper.getMainLooper()).post(() -> dataAvailable(page, data));
        }
    }
}
