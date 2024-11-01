package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.pdf_section.PdfFragment;

class ButtonPdf extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.pdf_books;
    }

    @Override
    Fragment getFragment() {
        return new PdfFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.pdf;
    }
}
