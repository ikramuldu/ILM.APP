package bn.poro.quran.hadith_section;

import android.app.Activity;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import bn.poro.quran.R;

class MyDrawerToggle extends ActionBarDrawerToggle {

    MyDrawerToggle(Activity context, DrawerLayout drawer, Toolbar toolbar) {
        super(context, drawer, toolbar, R.string.open, R.string.close);
    }
}
