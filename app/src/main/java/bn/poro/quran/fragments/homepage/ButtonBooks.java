package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.book_section.BookFragment;

 class ButtonBooks extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.books;
    }

    @Override
    Fragment getFragment() {
        return new BookFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.book;
    }
}
