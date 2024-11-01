package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.fragments.subject.SubjectFragment;


class ItemSubject extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.dictionary;
    }

    @Override
    Fragment getFragment() {
        return new SubjectFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.ic_topic;
    }
}
