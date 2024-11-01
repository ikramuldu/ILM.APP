package bn.poro.quran.book_section;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

public class ReadBookActivity extends AppCompatActivity {

    String bookID;
    LinearLayoutManager layoutManager;
    private final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.BOOK_READING_ACTIVITY);
        final SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        int themeId = store.getInt(Consts.THEME_KEY, 0);
        setTheme(SettingActivity.THEME[themeId]);
        setContentView(R.layout.activity_drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        Intent intent = getIntent();
        String title = intent.getStringExtra(Consts.TITLE_KEY);
        setTitle(title);
        bookID = intent.getStringExtra(Consts.ID_KEY);
        store.edit().putString(Consts.LATEST_BOOK_ID, bookID).putString(Consts.LATEST_BOOK_TITLE, title).apply();
        RecyclerView mainList = drawerLayout.findViewById(R.id.main_list);
        layoutManager = new MyLayoutManager(this);
        mainList.setLayoutManager(layoutManager);
        MainTextAdapter adapter = new MainTextAdapter(this, layoutManager, bookID);
        mainList.setAdapter(adapter);
        RecyclerView drawerList = findViewById(R.id.drawer_list);
        LinearLayoutManager linearLayoutManager = new MyLayoutManager(ReadBookActivity.this);
        drawerList.setLayoutManager(linearLayoutManager);
        DrawerAdapter drawerAdapter = new DrawerAdapter(ReadBookActivity.this, linearLayoutManager);
        drawerList.setAdapter(drawerAdapter);
        getOnBackPressedDispatcher().addCallback(this, callback);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                callback.setEnabled(false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                callback.setEnabled(true);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        adapter.resultAtPos = store.getInt(Consts.BOOK_POSITION_KEY + bookID, -1);
        if (adapter.resultAtPos == -1) {
            ((DrawerLayout) findViewById(R.id.drawer_layout)).openDrawer(GravityCompat.START);
        } else {
            layoutManager.scrollToPosition(adapter.resultAtPos);
            adapter.scrollOffset = store.getInt(Consts.BOOK_OFFSET_KEY + bookID, 0);
            if (!store.contains(Consts.LONG_PRESS_HINT)) {
                store.edit().putString(Consts.LONG_PRESS_HINT, "").apply();
                new AlertDialog.Builder(this)
                        .setMessage(R.string.pdf_menu_hint).setCancelable(false)
                        .setPositiveButton(R.string.ok, null).show();
            }
        }
    }

    @Override
    protected void onStop() {
        View view = ((RecyclerView) findViewById(R.id.main_list)).getChildAt(0);
        if (view != null) {
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit()
                    .putInt(Consts.BOOK_POSITION_KEY + bookID, (Integer) view.getTag())
                    .putInt(Consts.BOOK_OFFSET_KEY + bookID, view.getTop())
                    .apply();
        }
        super.onStop();
    }

    @Override
    protected void onStart() {
        if (RecreateManager.needRecreate(RecreateManager.BOOK_READING_ACTIVITY)) recreate();
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        if (!bookID.startsWith("a")) {
            getMenuInflater().inflate(R.menu.search, menu);
        }
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(this, SettingActivity.class));
        } else
            startActivity(new Intent(this, SearchBookActivity.class).putExtra(Consts.ID_KEY, bookID));
        return true;
    }
}
