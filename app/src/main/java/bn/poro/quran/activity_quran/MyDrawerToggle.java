package bn.poro.quran.activity_quran;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.R;
import bn.poro.quran.Utils;


class MyDrawerToggle extends ActionBarDrawerToggle {
    MyDrawerToggle(Activity context, DrawerLayout drawer, Toolbar toolbar) {
        super(context, drawer, toolbar, R.string.open, R.string.close);
    }


    @Override
    public void onDrawerOpened(View view) {
        super.onDrawerOpened(view);
        QuranActivity activity = (QuranActivity) Utils.getContext(view);
        ((RecyclerView) view).scrollToPosition(activity.currentSuraIndex);
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        if (QuranDrawerAdapter.keyboardVisible) {
            InputMethodManager imm = (InputMethodManager) drawerView.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(drawerView.getWindowToken(), 0);
            QuranDrawerAdapter.keyboardVisible = false;
        }
    }
}
