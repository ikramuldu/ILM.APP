package bn.poro.quran.hadith_section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

class HadithResultFragmentAdapter extends FragmentStateAdapter
        implements TabLayoutMediator.TabConfigurationStrategy {
    static final ArrayList<HadisResult> hadithResults = new ArrayList<>();
    private static int idOffsetForNewSearch;

    public HadithResultFragmentAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ResultFragment fragment = new ResultFragment();
        fragment.setArguments(hadithResults.get(position).toBundle());
        return fragment;
    }

    @Override
    public long getItemId(int position) {
        return position + idOffsetForNewSearch;
    }

    @Override
    public int getItemCount() {
        return hadithResults.size();
    }

    @Override
    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
        tab.setText(hadithResults.get(position).bookName);
    }

    public void resetFragments(ArrayList<HadisResult> results) {
        idOffsetForNewSearch += hadithResults.size();
        hadithResults.clear();
        hadithResults.addAll(results);
        notifyDataSetChanged();
    }
}
