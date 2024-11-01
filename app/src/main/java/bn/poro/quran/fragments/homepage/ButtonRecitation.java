package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.learning_list.LearningListFragment;

class ButtonRecitation extends ButtonModel {
    @Override
    public int getTitle() {
        return R.string.learn_arabic;
    }

    @Override
    Fragment getFragment() {
        return new LearningListFragment();
    }

    @Override
    public int getIcon() {
        return R.drawable.laters;
    }
}
