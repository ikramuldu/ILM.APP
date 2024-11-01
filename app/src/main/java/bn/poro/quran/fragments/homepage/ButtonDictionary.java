package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.understand.UnderstandListFragment;

class ButtonDictionary extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.understand_quran;
    }

    @Override
    Fragment getFragment() {
        return new UnderstandListFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_dictionary;
    }
}
