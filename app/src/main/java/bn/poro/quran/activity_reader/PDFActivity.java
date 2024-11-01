package bn.poro.quran.activity_reader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_reader.pdfviewer.PDFView;
import bn.poro.quran.activity_reader.pdfviewer.listener.OnErrorListener;
import bn.poro.quran.activity_reader.pdfviewer.listener.OnLongPressListener;
import bn.poro.quran.activity_reader.pdfviewer.listener.OnRenderListener;
import bn.poro.quran.activity_reader.pdfviewer.scroll.DefaultScrollHandle;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.pdf_section.FavFragment;


public class PDFActivity extends AppCompatActivity implements View.OnClickListener,
        PopupMenu.OnMenuItemClickListener,
        TextView.OnEditorActionListener,
        DialogInterface.OnCancelListener,
        OnErrorListener, OnRenderListener,
        DialogInterface.OnClickListener,
        CompoundButton.OnCheckedChangeListener, OnLongPressListener {
    private static final int NIGHT_THEME_MASK = 2;
    private static final int HORIZONTAL_MASK = 4;
    private static final int DOUBLE_PAGE_MASK = 8;
    private static final int FULLSCREEN_MASK = 16;
    private static final int LOCK_PAGE_MASK = 32;
    private static final int LANDSCAPE_MASK = 64;
    private static final String DEF_ZOOM_POS = "1\n0\n0";

    private int config;
    private AlertDialog dialog;
    private String password;
    private int id;
    private boolean savePassword, quality, antialias;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.PDF_ACTIVITY);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (Utils.dataPath == null) {
            Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        }
        int theme = store.getInt(Consts.THEME_KEY, 0);
        Intent intent = getIntent();
        id = intent.getIntExtra(Consts.ID_KEY, -1);
        String bookName = intent.getStringExtra(Consts.LATEST_PDF_NAME);
        if (bookName == null) {
            String path = getPathFromURI(intent.getData());
            try {
                path = URLDecoder.decode(path, "UTF-8");
            } catch (Exception e) {
                L.d(e);
            }
            int end = path.indexOf('?');
            if (end == -1) end = path.length();
            int start = path.lastIndexOf('/', end);
            bookName = path.substring(start + 1, end);
        }
        String[] strings = store.getString(bookName + id, "").split("\n");
        if (strings.length > 3)
            config = Integer.parseInt(strings[3]);
        setTheme(SettingActivity.THEME[theme]);
        if (theme == 1 || theme == 4) {
            config = config | NIGHT_THEME_MASK;
        } else config = config & ~NIGHT_THEME_MASK;
        if ((config & LANDSCAPE_MASK) == LANDSCAPE_MASK)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        savePassword = store.getBoolean(Consts.SAVE_PASSWORD, true);
        quality = store.getBoolean(Consts.QUALITY, false);
        antialias = store.getBoolean(Consts.ANTIALIAS, true);
        if (password == null && strings.length > 4) password = strings[4];
        setContentView(R.layout.activity_pdf);
        setSupportActionBar(findViewById(R.id.toolbar));
        setTitle(bookName);
        if ((config & FULLSCREEN_MASK) == FULLSCREEN_MASK) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) actionBar.hide();
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 0x00000004 | 0x00001000);
        }
        initView();
    }

    private String getPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) try {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            cursor.moveToFirst();
            String s = cursor.getString(idx);
            cursor.close();
            if (s != null) return s;
        } catch (Exception e) {
            L.d(e);
        }
        String path = uri.getPath();
        if (path == null) return String.valueOf(uri);
        return path;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RecreateManager.needRecreate(RecreateManager.PDF_ACTIVITY)) recreate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveState();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        menu.findItem(R.id.antialias).setChecked(antialias);
        menu.findItem(R.id.quality).setChecked(quality);
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        PDFView pdfView = findViewById(R.id.pdf_view);
        if (id == R.id.setting) startActivity(new Intent(this, SettingActivity.class));
        else if (id == R.id.antialias) {
            antialias = !item.isChecked();
            pdfView.enableAntialiasing(antialias);
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit()
                    .putBoolean(Consts.ANTIALIAS, antialias).apply();
        } else if (id == R.id.quality) {
            quality = !item.isChecked();
            pdfView.setQuality(quality);
            getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit()
                    .putBoolean(Consts.QUALITY, quality).apply();
        }
        return true;
    }

    private void saveState() {
        PDFView pdfView = findViewById(R.id.pdf_view);
        try {
            SharedPreferences.Editor editor = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE).edit();
            String string = pdfView.getZoom() + "\n" +
                    (pdfView.getCurrentXOffset() / pdfView.getWidth()) + "\n" +
                    pdfView.getPositionOffset() + "\n" + config;
            if (password != null) {
                editor.putBoolean(Consts.SAVE_PASSWORD, savePassword);
                if (savePassword) string = string + "\n" + password;
            }
            editor.putString(getTitle().toString() + id, string);
            editor.apply();
        } catch (NullPointerException e) {
            L.d(e);
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.save) {
            saveBookmark((TextView) v.getTag());
        }
    }

    private void saveBookmark(TextView textView) {
        String message = textView.getText().toString();
        textView.clearFocus();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        PDFView pdfView = findViewById(R.id.pdf_view);
        String name = getTitle().toString();
        String data = pdfView.getZoom() + "\n" +
                (pdfView.getCurrentXOffset() / pdfView.getWidth()) + "\n" +
                pdfView.getPositionOffset() + "\n" + config;
        database.execSQL("insert into pdf_mark values(?,?,?,?,?)",
                new Object[]{id, System.currentTimeMillis(), message, data, name});
        FavFragment.refreshRequired = true;
        database.close();
        dialog.dismiss();
        dialog = null;
        if (id == -1) {
            File file = new File(Utils.dataPath, name);
            if (!file.exists()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Uri uri = getIntent().getData();
                            if (uri != null)
                                Utils.copy(getContentResolver().openInputStream(uri), file);
                        } catch (Exception e) {
                            L.d(e);
                        }
                    }
                }.start();
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        PDFView pdfView = findViewById(R.id.pdf_view);
        int id = item.getItemId();
        if (id == R.id.bookmark) {
            View view = getLayoutInflater().inflate(R.layout.edit_title, null);
            View saveButton = view.findViewById(R.id.save);
            saveButton.setOnClickListener(this);
            EditText editText = view.findViewById(R.id.edit_text);
            editText.requestFocus();
            saveButton.setTag(editText);
            editText.setOnEditorActionListener(this);
            dialog = new AlertDialog.Builder(this)
                    .setView(view)
                    .show();
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } else if (id == R.id.double_page) {
            if (item.isChecked()) config = config & ~DOUBLE_PAGE_MASK;
            else config = config | DOUBLE_PAGE_MASK;
            initView();
        } else if (id == R.id.lock_screen) {
            if (item.isChecked()) config = config & ~LOCK_PAGE_MASK;
            else config = config | LOCK_PAGE_MASK;
            pdfView.setLocked(!item.isChecked());
        } else if (id == R.id.goto_page) {
            EditText editText = (EditText) getLayoutInflater().inflate(R.layout.page_num, null);
            editText.requestFocus();
            editText.setOnEditorActionListener(this);
            dialog = new AlertDialog.Builder(this)
                    .setView(editText)
                    .setOnCancelListener(this)
                    .show();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            float density = getResources().getDisplayMetrics().density;
            if (dialog.getWindow() != null)
                dialog.getWindow().setLayout((int) (110 * density), (int) (80 * density));
        } else if (id == R.id.swipe_horizontal) {
            if (item.isChecked()) config = config & ~HORIZONTAL_MASK;
            else config = config | HORIZONTAL_MASK;
            initView();
        } else if (id == R.id.landscape) {
            if (item.isChecked()) {
                config = config & ~LANDSCAPE_MASK;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                config = config | LANDSCAPE_MASK;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        } else {
            ActionBar actionBar = getSupportActionBar();
            Window window = getWindow();
            if (item.isChecked()) {
                config = config & ~FULLSCREEN_MASK;
                if (actionBar != null) actionBar.show();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.getDecorView().setSystemUiVisibility(0x00000100);
            } else {
                config = config | FULLSCREEN_MASK;
                if (actionBar != null) actionBar.hide();
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 0x00000004 | 0x00001000);
            }
        }
        return true;
    }

    private void initView() {
        PDFView pdfView = findViewById(R.id.pdf_view);
        PDFView.Configurator configurator;
        L.d(String.valueOf(id));
        Uri uri = getIntent().getData();
        if (uri != null) configurator = pdfView.fromUri(getIntent().getData());
        else if (id >= 0) configurator = pdfView.fromFile(new File(Utils.dataPath + id + ".pdf"));
        else configurator = pdfView.fromFile(new File(Utils.dataPath + getTitle()));
        configurator.onError(this).onRender(this)
                .onLongPress(this)
                .password(password).highQuality(quality).enableAntialiasing(antialias);
        configurator.scrollHandle(new DefaultScrollHandle(this));
        configurator.nightMode((config & NIGHT_THEME_MASK) == NIGHT_THEME_MASK);
        configurator.doublePage((config & DOUBLE_PAGE_MASK) == DOUBLE_PAGE_MASK);
        if ((config & HORIZONTAL_MASK) == HORIZONTAL_MASK)
            configurator.pageSnap(true).autoSpacing(true).pageFling(true).swipeHorizontal(true);
        configurator.load();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        String string = v.getText().toString();
        if (v.getId() == R.id.save) saveBookmark(v);
        else if (v.getId() == R.id.password) {
            password = string;
            initView();
        } else {
            if (string.isEmpty()) {
                Toast.makeText(this, "Enter page number!", Toast.LENGTH_SHORT).show();
                return true;
            }
            int page = Integer.parseInt(string);
            PDFView pdfView = findViewById(R.id.pdf_view);
            if (page > pdfView.getPageCount()) {
                Toast.makeText(this, "maximum pages " + pdfView.getPageCount(), Toast.LENGTH_SHORT).show();
                return true;
            }
            pdfView.jumpTo(page - 1);
        }
        dialog.dismiss();
        dialog = null;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
        return true;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onError(Throwable throwable) {
        String message = throwable.getMessage();
        if (message != null && message.contains("password")) {
            View view = getLayoutInflater().inflate(R.layout.password, null);
            EditText editText = view.findViewById(R.id.password);
            editText.setOnEditorActionListener(this);
            CheckBox checkBox = view.findViewById(R.id.checkbox);
            checkBox.setChecked(savePassword);
            checkBox.setOnCheckedChangeListener(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            if (password == null) builder.setTitle(R.string.enter_pass);
            else builder.setTitle(R.string.wrong_pass);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(R.string.negative, this);
            builder.setView(view);
            dialog = builder.show();
        } else Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInitiallyRendered(int pages) {
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        if (!store.contains(Consts.PDF_MENU_OPEN_HINTED)) {
            store.edit().putString(Consts.PDF_MENU_OPEN_HINTED, "").apply();
            new AlertDialog.Builder(this).setMessage(R.string.pdf_menu_hint)
                    .setPositiveButton(R.string.okay, null).show();
        }
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        String s = store.getString(getTitle().toString() + id, DEF_ZOOM_POS);
        String[] strings = s.split("\n");
        PDFView pdfView = drawerLayout.findViewById(R.id.pdf_view);
        pdfView.zoomTo(Float.parseFloat(strings[0]));
        pdfView.moveTo(Float.parseFloat(strings[1]) * pdfView.getWidth(), 0, false);
        pdfView.setPositionOffset(Float.parseFloat(strings[2]), true);
        if ((config & HORIZONTAL_MASK) == HORIZONTAL_MASK) pdfView.performPageSnap();
        pdfView.setLocked((config & LOCK_PAGE_MASK) == LOCK_PAGE_MASK);
        RecyclerView drawerList = drawerLayout.findViewById(R.id.drawer_list);
        List<PdfDocument.Bookmark> bookmarks = pdfView.getTableOfContents();
        if (bookmarks.isEmpty()) drawerList.setVisibility(View.GONE);
        else {
            drawerList.setAdapter(new ContentAdapter(this, bookmarks));
            drawerList.setLayoutManager(new MyLayoutManager(this));
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, drawerLayout.findViewById(R.id.toolbar), R.string.open, R.string.close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            TextView textView = ((AlertDialog) dialog).findViewById(R.id.password);
            assert textView != null;
            password = textView.getText().toString();
            initView();
        } else finish();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        savePassword = isChecked;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        View view = findViewById(R.id.fab);
        if (e != null) {
            view.setX(e.getX());
            view.setY(e.getY());
        }
        PopupMenu popupMenu = new PopupMenu(this, view);
        Menu menu = popupMenu.getMenu();
        popupMenu.getMenuInflater().inflate(R.menu.pdf_popup, menu);
        menu.findItem(R.id.swipe_horizontal).setChecked((config & HORIZONTAL_MASK) == HORIZONTAL_MASK);
        menu.findItem(R.id.double_page).setChecked((config & DOUBLE_PAGE_MASK) == DOUBLE_PAGE_MASK);
        menu.findItem(R.id.full_screen).setChecked((config & FULLSCREEN_MASK) == FULLSCREEN_MASK);
        menu.findItem(R.id.lock_screen).setChecked((config & LOCK_PAGE_MASK) == LOCK_PAGE_MASK);
        menu.findItem(R.id.landscape).setChecked((config & LANDSCAPE_MASK) == LANDSCAPE_MASK);
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(this);
    }
}
