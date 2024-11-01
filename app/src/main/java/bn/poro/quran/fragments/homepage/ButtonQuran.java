package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.quran.QuranFragment;

class ButtonQuran extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.quran;
    }

    @Override
    Fragment getFragment() {
        return new QuranFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_quran;
    }
}
