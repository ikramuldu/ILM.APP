package bn.poro.quran.book_section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import bn.poro.quran.R;

class PageAdapter extends FragmentStateAdapter implements TabLayoutMediator.TabConfigurationStrategy {

    public PageAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if(position==0)return new NamesFragment();
        return new BookmarkFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    @Override
    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
        tab.setText(position==0? R.string.books:R.string.menu_bookmarks);
    }
}
