package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.science.ScienceFragment;


class ItemScience extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.science;
    }

    @Override
    Fragment getFragment() {
        return new ScienceFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_science;
    }
}
