package bn.poro.quran.activity_quran;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import bn.poro.quran.Consts;

class SuraPageAdapter extends FragmentStateAdapter {

    private final boolean rtl;

    public SuraPageAdapter(FragmentManager manager, Lifecycle lifecycle, boolean rtl) {
        super(manager,lifecycle);
        this.rtl = rtl;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = new SuraViewFragment();
        Bundle args = new Bundle();
        args.putInt(Consts.EXTRA_SURA_ID, rtl ? Consts.SURA_COUNT - 1 - position : position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return Consts.SURA_COUNT;
    }
}
