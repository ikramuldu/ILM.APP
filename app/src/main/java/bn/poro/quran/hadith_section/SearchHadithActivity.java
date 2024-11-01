package bn.poro.quran.hadith_section;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.util.ArrayList;

import bn.poro.quran.Consts;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.views.InstantCompleteView;

public class SearchHadithActivity extends AppCompatActivity implements TextView.OnEditorActionListener, AdapterView.OnItemClickListener, View.OnClickListener, InitAutoCompleteTask.SearchHistoryListener, SearchTask.HadisSearchTaskListener, DialogInterface.OnClickListener {

    public SearchDrawerAdapter drawerAdapter;
    private String searchString;
    private HadithResultFragmentAdapter fragmentAdapter;
    private SearchTask searchTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.HADIS_SEARCH_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.activity_hadis_search);
        new InitAutoCompleteTask(this).start();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, @NonNull View view, int position, long id) {
        EditText editText = findViewById(R.id.search);
        searchString = ((TextView) view.findViewById(R.id.text)).getText().toString();
        editText.setText(searchString);
        search();
    }

    @Override
    public boolean onEditorAction(@NonNull TextView v, int actionId, KeyEvent event) {
        searchString = v.getText().toString();
        if (actionId == EditorInfo.IME_ACTION_SEARCH && !searchString.trim().isEmpty()) {
            search();
        }
        return false;
    }

    private void search() {
        if (searchTask != null) searchTask.cancel();
        searchTask = new SearchTask(searchString, getIntent().getIntExtra(Consts.ID_KEY, -1), this);
        searchTask.start();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        AutoCompleteTextView view = findViewById(R.id.search);
        view.clearFocus();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        AutoCompleteAdapter autoCompleteAdapter = (AutoCompleteAdapter) view.getAdapter();
        autoCompleteAdapter.refresh(searchString);
    }

    @Override
    protected void onStop() {
        super.onStop();
        AutoCompleteTextView textView = findViewById(R.id.search);
        AutoCompleteAdapter autoCompleteAdapter = (AutoCompleteAdapter) textView.getAdapter();
        autoCompleteAdapter.saveAt(new File(Utils.dataPath, Consts.SEARCH_HISTORY_FILE));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RecreateManager.needRecreate(RecreateManager.HADIS_SEARCH_ACTIVITY)) {
            recreate();
        }
    }


    @Override
    public void onClick(@NonNull View v) {
        if (v.getId() == R.id.delete) {
            TextView textView = findViewById(R.id.search);
            textView.setText("");
            textView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } else if (v.getId() == R.id.more && !HadithResultFragmentAdapter.hadithResults.isEmpty()) {
            new AlertDialog.Builder(this).setAdapter(new ResultBookListAdapter(HadithResultFragmentAdapter.hadithResults), this).show();
        }
    }

    @Override
    public void onSearchHistoryReady(ArrayList<String> list) {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        InstantCompleteView editText = drawerLayout.findViewById(R.id.search);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        editText.setAdapter(new AutoCompleteAdapter(list));
        editText.setOnItemClickListener(this);
        editText.setOnEditorActionListener(this);
        drawerLayout.findViewById(R.id.delete).setOnClickListener(this);
        if (!list.isEmpty()) {
            searchString = list.get(list.size() - 1);
        }
        ViewPager2 pager2 = drawerLayout.findViewById(R.id.view_pager2);
        fragmentAdapter = new HadithResultFragmentAdapter(this);
        if (!HadithResultFragmentAdapter.hadithResults.isEmpty()) editText.setText(searchString);
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        drawerList.setLayoutManager(new MyLayoutManager(this));
        drawerLayout.findViewById(R.id.more).setOnClickListener(this);
        drawerAdapter = new SearchDrawerAdapter();
        drawerList.setAdapter(drawerAdapter);
        pager2.setAdapter(fragmentAdapter);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        View tab = drawerLayout.findViewById(R.id.name_bar);
        if (getIntent().getIntExtra(Consts.ID_KEY, -1) != -1) tab.setVisibility(View.GONE);
        else new TabLayoutMediator(tab.findViewById(R.id.tabs), pager2, fragmentAdapter).attach();
    }

    @Override
    public void onSearchCompleted(ArrayList<HadisResult> results, int count) {
        fragmentAdapter.resetFragments(results);
        if (count == 0)
            Toast.makeText(this, R.string.not_found, Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, count + " টি হাদিস পাওয়া গেছে", Toast.LENGTH_SHORT).show();
    }

    public String getSearchString() {
        return searchString;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ((ViewPager2) findViewById(R.id.view_pager2)).setCurrentItem(which, false);
    }
}
