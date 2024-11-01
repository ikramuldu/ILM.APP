package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.name.NameFragment;

class ItemNames extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.names;
    }

    @Override
    Fragment getFragment() {
        return new NameFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_names;
    }
}
