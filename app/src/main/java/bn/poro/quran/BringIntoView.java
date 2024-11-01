package bn.poro.quran;

import androidx.recyclerview.widget.LinearLayoutManager;

public class BringIntoView implements Runnable {
    private final LinearLayoutManager layoutManager;
    private final int firstInsertedItem;

    public BringIntoView(LinearLayoutManager layoutManager, int firstInsertedItem) {
        this.layoutManager = layoutManager;
        this.firstInsertedItem = firstInsertedItem;
    }

    @Override
    public void run() {
        int lastItem = layoutManager.findLastVisibleItemPosition();
        if (lastItem <= firstInsertedItem) layoutManager.scrollToPosition(firstInsertedItem);
    }
}
