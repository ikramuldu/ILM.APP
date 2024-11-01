package bn.poro.quran.fragments.homepage;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import bn.poro.quran.Utils;

abstract class ButtonModel implements View.OnClickListener {
    abstract int getTitle();

    @Override
    public void onClick(View v) {
        Utils.replaceFragment((FragmentActivity) Utils.getContext(v), getFragment());
    }

    Fragment getFragment() {
        return null;
    }

    abstract int getIcon();
}
