package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.sajdah.SajdahFragment;

class ItemSajdah extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.sajdah;
    }

    @Override
    Fragment getFragment() {
        return new SajdahFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_sajdah;
    }
}
