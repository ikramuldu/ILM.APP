package bn.poro.quran.media_section;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

class PageAdapter extends FragmentStateAdapter {
    PageAdapter(MediaHomeActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        IslamhouseFragment fragment = new IslamhouseFragment();
        Bundle args = new Bundle();
        args.putInt("p", position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
