package bn.poro.quran.hadith_section;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_setting.SettingActivity;

public class ReadHadisActivity extends AppCompatActivity {

    int bookID;
    LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.HADIS_READING_ACTIVITY);
        final SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setContentView(R.layout.activity_drawer);
        setSupportActionBar(findViewById(R.id.toolbar));
        bookID = getIntent().getIntExtra(Consts.ID_KEY, 0);
        setTitle(getIntent().getStringExtra(Consts.TITLE_KEY));
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        RecyclerView mainList = drawerLayout.findViewById(R.id.main_list);
        layoutManager = new MyLayoutManager(this);
        mainList.setLayoutManager(layoutManager);
        HadisTextAdapter adapter = new HadisTextAdapter(this, layoutManager);
        mainList.setAdapter(adapter);
        MyDrawerToggle drawerToggle = new MyDrawerToggle(this, drawerLayout, findViewById(R.id.toolbar));
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        LinearLayoutManager drawerListManager = new MyLayoutManager(this);
        drawerList.setLayoutManager(drawerListManager);
        drawerList.setAdapter(new DrawerAdapter(this, drawerListManager));
        int hadis = getIntent().getIntExtra(Consts.HADITH_NO, -1);
        if (hadis != -1) {
            mainList.post(() -> layoutManager.scrollToPositionWithOffset(hadis, 0));
        } else if (!store.contains(Consts.HADIS_CHAPTER_KEY + bookID)) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            final int chapter = store.getInt(Consts.HADIS_CHAPTER_KEY + bookID, 0);
            final int position = store.getInt(Consts.HADIS_POSITION_KEY + bookID, 0);
            mainList.post(() -> layoutManager.scrollToPositionWithOffset(chapter, position));
        }
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    protected void onStop() {
        View view = ((RecyclerView) findViewById(R.id.main_list)).getChildAt(0);
        if (view != null) {
            int position = layoutManager.getPosition(view);
            SharedPreferences.Editor editor = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit();
            editor.putInt(Consts.HADIS_CHAPTER_KEY + bookID, position);
            editor.putInt(Consts.HADIS_POSITION_KEY + bookID, view.getTop());
            editor.apply();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        if (RecreateManager.needRecreate(RecreateManager.HADIS_READING_ACTIVITY)) recreate();
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting)
            startActivity(new Intent(this, SettingActivity.class));
        else
            startActivity(new Intent(this, SearchHadithActivity.class).putExtra(Consts.ID_KEY, bookID));
        return true;
    }
}
