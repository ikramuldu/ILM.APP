package bn.poro.quran.book_section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

class SearchFragmentAdapter extends FragmentStateAdapter
        implements TabLayoutMediator.TabConfigurationStrategy {
    static final ArrayList<SearchResult> searchResults = new ArrayList<>();
    private static int IdOffsetForNewSearch;

    public SearchFragmentAdapter(@NonNull SearchBookActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(searchResults.get(position).toBundle());
        return fragment;
    }

    @Override
    public long getItemId(int position) {
        return position + IdOffsetForNewSearch;
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    @Override
    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
        tab.setText(searchResults.get(position).bookName);
    }

    public void updateOffset(int count) {
        IdOffsetForNewSearch += count;
    }
}
