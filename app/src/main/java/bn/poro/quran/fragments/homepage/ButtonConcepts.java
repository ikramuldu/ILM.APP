package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.topic.TopicFragment;

class ButtonConcepts extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.menu_quran_index;
    }

    @Override
    Fragment getFragment() {
        return new TopicFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_concept;
    }
}
