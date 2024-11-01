package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.hadith_section.HadisFragment;

 class ButtonHadis extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.hadis;
    }
     @Override
    Fragment getFragment() {
        return new HadisFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.hadis;
    }
}
