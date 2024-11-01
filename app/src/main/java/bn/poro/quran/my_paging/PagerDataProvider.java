package bn.poro.quran.my_paging;

import java.util.List;

public interface PagerDataProvider<ITEM> {
    List<ITEM> fetch(int request);

    void init();
}
