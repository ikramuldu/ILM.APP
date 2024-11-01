package bn.poro.quran.fragments.quran;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import bn.poro.quran.R;
import bn.poro.quran.fragments.bookmark.BookmarkFragment;
import bn.poro.quran.fragments.para_list.ParaFragment;
import bn.poro.quran.fragments.sura_list.SuraListFragment;

class PageAdapter extends FragmentStateAdapter implements TabLayoutMediator.TabConfigurationStrategy {
    PageAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SuraListFragment();
            case 1:
                return new ParaFragment();
            default:
                return new BookmarkFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @Override
    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
        switch (position) {
            case 0:
                tab.setText(R.string.quran_sura);
                break;
            case 1:
                tab.setText(R.string.quran_juz2);
                break;
            case 2:
                tab.setText(R.string.menu_bookmarks);
        }
    }
}
