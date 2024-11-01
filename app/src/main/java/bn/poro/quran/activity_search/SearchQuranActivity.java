package bn.poro.quran.activity_search;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.views.InstantCompleteView;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_setting.SettingActivity;

public class SearchQuranActivity extends AppCompatActivity implements
        TextView.OnEditorActionListener,
        SearchTask.Listener,
        AdapterView.OnItemClickListener,
        View.OnClickListener, RemoveHarakaTask.Listener {

    public String string;
    public String[] suraNames;
    private SearchTask searchTask;
    private SearchAdapter searchAdapter;
    private SearchDrawerAdapter drawerAdapter;
    LinearLayoutManager layoutManager;
    private AutoCompleteAdapter autoCompleteAdapter;
    private AlertDialog dialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.QURAN_SEARCH_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        setTheme(SettingActivity.THEME[store.getInt(Consts.THEME_KEY, 0)]);
        setContentView(R.layout.activity_search);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from quran limit 1", null);
        int count = cursor.getColumnCount();
        cursor.close();
        database.close();
        if (count == 6) {
            dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.loading)
                    .setCancelable(false)
                    .setTitle("Preparing database").show();
            new RemoveHarakaTask(this).start();
        } else onDatabaseReady();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        EditText editText = findViewById(R.id.search);
        string = ((TextView) view.findViewById(R.id.text)).getText().toString();
        editText.setText(string);
        search();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        string = v.getText().toString();
        if (actionId == EditorInfo.IME_ACTION_SEARCH && !string.trim().isEmpty()) {
            search();
        }
        return false;
    }

    private void search() {
        if (searchTask != null) searchTask.cancel();
        searchTask = new SearchTask(string, this);
        Toast.makeText(this, getString(R.string.searching, string), Toast.LENGTH_SHORT).show();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = findViewById(R.id.search);
        view.clearFocus();
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        autoCompleteAdapter.add(string);
        searchTask.start();
    }

    @Override
    public void onFinish(@NonNull ArrayList<SearchTask.SearchResult> searchResult, SearchTask.ResultItem[] mainCursor) {
        searchTask = null;
        Toast.makeText(this, getString(R.string.search_result, string, mainCursor.length), Toast.LENGTH_SHORT).show();
        searchAdapter.update(searchResult, mainCursor);
        drawerAdapter.update(mainCursor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        autoCompleteAdapter.saveTo(new File(Utils.dataPath, Consts.SEARCH_HISTORY_FILE));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.setting) {
            startActivity(new Intent(this, SettingActivity.class));
        }
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
        if (RecreateManager.needRecreate(RecreateManager.QURAN_SEARCH_ACTIVITY)) {
            findViewById(R.id.search).clearFocus();
            recreate();
        }
    }

    @Override
    public void onClick(View v) {
        TextView textView = findViewById(R.id.search);
        textView.setText("");
        textView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onDatabaseReady() {
        if (dialog != null)
            dialog.dismiss();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = drawerLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        InstantCompleteView editText = toolbar.findViewById(R.id.search);
        autoCompleteAdapter = new AutoCompleteAdapter();
        editText.setAdapter(autoCompleteAdapter);
        editText.setOnItemClickListener(this);
        suraNames = getResources().getStringArray(R.array.sura_transliteration);
        editText.setOnEditorActionListener(this);
        toolbar.findViewById(R.id.delete).setOnClickListener(this);
        File file = new File(Utils.dataPath, Consts.SEARCH_HISTORY_FILE);
        if (file.length() != 0)
            try {
                InputStream inputStream;
                inputStream = Files.newInputStream(file.toPath());
                byte[] bytes = new byte[inputStream.available()];
                if (inputStream.read(bytes) > 0) {
                    String[] items = new String(bytes).split("\n");
                    autoCompleteAdapter.addAll(Arrays.asList(items));
                    string = items[items.length - 1];
                    editText.setText(string);
                    search();
                }
                inputStream.close();
            } catch (Exception e) {
                L.d(e);
            }
        RecyclerView mainList = drawerLayout.findViewById(R.id.main_list);
        searchAdapter = new SearchAdapter(this);
        layoutManager = new MyLayoutManager(this);
        mainList.setLayoutManager(layoutManager);
        mainList.setAdapter(searchAdapter);
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        drawerAdapter = new SearchDrawerAdapter(this);
        drawerList.setAdapter(drawerAdapter);
        drawerList.setLayoutManager(new MyLayoutManager(this));
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }
}
