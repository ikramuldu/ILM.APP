package bn.poro.quran.book_section;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.views.InstantCompleteView;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;

public class SearchBookActivity extends AppCompatActivity implements TextView.OnEditorActionListener, SearchTask.Listener, AdapterView.OnItemClickListener, View.OnClickListener, DialogInterface.OnClickListener {

    public String searchText;
    SearchTask searchTask;
    private TitleAdapter titleAdapter;
    private SearchFragmentAdapter searchFragmentAdapter;
    private SearchHistoryAdapter historyAdapter;

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        RecreateManager.recreated(RecreateManager.BOOK_SEARCH_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.activity_search_book);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        InstantCompleteView editText = toolbar.findViewById(R.id.search);
        historyAdapter = new SearchHistoryAdapter(getLayoutInflater());
        editText.setAdapter(historyAdapter);
        editText.setOnItemClickListener(this);
        editText.setOnEditorActionListener(this);
        View tab = findViewById(R.id.name_bar);
        ViewPager2 pager = findViewById(R.id.view_pager2);
        searchFragmentAdapter = new SearchFragmentAdapter(this);
        pager.setAdapter(searchFragmentAdapter);
        if (getIntent().getStringExtra(Consts.ID_KEY) != null) tab.setVisibility(View.GONE);
        else
            new TabLayoutMediator(tab.findViewById(R.id.tabs), pager, searchFragmentAdapter).attach();
        findViewById(R.id.more).setOnClickListener(this);
        findViewById(R.id.delete).setOnClickListener(this);
        File file = new File(Utils.dataPath + Consts.SEARCH_HISTORY_FILE);
        if (file.length() != 0) {
            try {
                InputStream inputStream = Files.newInputStream(file.toPath());
                byte[] bytes = new byte[inputStream.available()];
                if (inputStream.read(bytes) > 0) {
                    String[] items = new String(bytes).split("\n");
                    historyAdapter.mainList.addAll(Arrays.asList(items));
                    searchText = items[items.length - 1];
                }
                inputStream.close();
            } catch (Exception e) {
                L.d(e);
            }
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        searchText = ((TextView) view.findViewById(R.id.text)).getText().toString();
        search();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        searchText = v.getText().toString();
        if (actionId == EditorInfo.IME_ACTION_SEARCH && !searchText.trim().isEmpty()) {
            search();
        }
        return false;
    }

    private void search() {
        if (searchTask != null) searchTask.cancel();
        String id = getIntent().getStringExtra(Consts.ID_KEY);
        String path = Utils.dataPath + Consts.BOOK_SUB_PATH;
        if (id != null) path += id;
        searchTask = new SearchTask(path, searchText, this);
        Toast.makeText(this, R.string.searching, Toast.LENGTH_SHORT).show();
        int count = SearchFragmentAdapter.searchResults.size();
        SearchFragmentAdapter.searchResults.clear();
        searchFragmentAdapter.updateOffset(count);
        searchFragmentAdapter.notifyItemRangeRemoved(0, count);
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = findViewById(R.id.search);
        view.clearFocus();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        historyAdapter.mainList.remove(searchText);
        historyAdapter.mainList.add(searchText);
        searchTask.start();
    }

    @Override
    public void onProgress(ArrayList<SearchResult> searchResult) {
        if (searchResult == null) {
            searchTask = null;
            String msg;
            if (SearchFragmentAdapter.searchResults.isEmpty())
                msg = "No match found";
            else msg = "Search completed";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            int start = SearchFragmentAdapter.searchResults.size();
            SearchFragmentAdapter.searchResults.addAll(searchResult);
            searchFragmentAdapter.notifyItemRangeInserted(start, searchResult.size());
            searchResult.clear();
            if (titleAdapter != null) titleAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            OutputStream outputStream = Files.newOutputStream(Paths.get(Utils.dataPath + Consts.SEARCH_HISTORY_FILE));
            int len = historyAdapter.mainList.size();
            for (int i = 0; i < len; i++) {
                outputStream.write(historyAdapter.mainList.get(i).getBytes());
                outputStream.write('\n');
            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            L.d(e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(this, SettingActivity.class));
        } else super.onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RecreateManager.needRecreate(RecreateManager.BOOK_SEARCH_ACTIVITY)) {
            findViewById(R.id.search).clearFocus();
            recreate();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.delete) {
            TextView textView = findViewById(R.id.search);
            textView.setText("");
            textView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            return;
        }
        if (SearchFragmentAdapter.searchResults.isEmpty()) return;
        titleAdapter = new TitleAdapter(SearchFragmentAdapter.searchResults);
        new AlertDialog.Builder(this).setAdapter(titleAdapter, this).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ((ViewPager2) findViewById(R.id.view_pager2)).setCurrentItem(which, false);
        titleAdapter = null;
    }
}
