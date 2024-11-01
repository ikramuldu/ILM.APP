package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.dua.DuaFragment;


 class ItemDua extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.dua;
    }

    @Override
    Fragment getFragment() {
        return new DuaFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_dua;
    }
}
