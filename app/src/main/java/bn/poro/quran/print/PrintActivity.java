package bn.poro.quran.print;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.Codes;
import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_reader.PDFActivity;
import bn.poro.quran.activity_setting.SettingActivity;

public class PrintActivity extends AppCompatActivity implements PdfCreator.Listener, DialogInterface.OnClickListener {

    ArrayList<SuraInfo> suraInfo;
    int tabTextColor;
    int banglaColumn = -1;
    float banglaFontSize, arabicFontSize;
    Typeface arabicFont, kalpurush, quranBangla;
    Matcher qalqala, iqlab, idgham, ikhfa, ghunna;
    private boolean justifyTrans;
    Stack<TextView> viewPool;
    TransData[] transData;
    Cursor wordCursor;
    Cursor mainCursor;
    private AlertDialog dialog;
    private PrintQuranAdapter fullQuranAdapter;
    private PdfCreator task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RecreateManager.recreated(RecreateManager.QURAN_ACTIVITY);
        new Thread() {
            @Override
            public void run() {
                initData();
            }
        }.start();
        setContentView(R.layout.activity_quran);
        dialog = new AlertDialog.Builder(this).setView(R.layout.loading).setCancelable(false).show();
    }

    private void getWord() {
        SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = sqLiteDatabase.rawQuery("select id from status where id<? and extra=1 order by id=5003 desc,id=5002 desc", new String[]{String.valueOf(DownloadService.TRANS_START_INDEX)});
        int count = cursor.getCount();
        if (count == 0) return;
        cursor.moveToFirst();
        if (cursor.getInt(0) == DownloadService.WORD_START_INDEX) banglaColumn = 0;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + cursor.getString(0) + ".db", null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        StringBuilder args = new StringBuilder("main.content.text");
        StringBuilder tables = new StringBuilder("main.content");
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            if (banglaColumn == -1 && id.equals("5000")) banglaColumn = cursor.getPosition();
            database.execSQL("ATTACH DATABASE ? AS w" + id, new String[]{Utils.dataPath + id + ".db"});
            args.append(",w");
            args.append(id);
            args.append(".content.text");
            tables.append(" inner join w");
            tables.append(id);
            tables.append(".content on main.content.rowid=w");
            tables.append(id);
            tables.append(".content.rowid");
        }
        wordCursor = database.rawQuery("select " + args + " from " + tables, null);
        cursor.close();
        sqLiteDatabase.close();
    }

    private void getTrans() {
        SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        sqLiteDatabase.execSQL("ATTACH DATABASE ? AS sts", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
        Cursor cursor = sqLiteDatabase.rawQuery("select id,lang||'- '||name from trans " + "inner join status using (id) where extra=1 order by id=5203 desc,id=5202 desc", null);
        int count = cursor.getCount();
        if (count == 0) return;
        transData = new TransData[count];
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            String name = cursor.getString(1);
            int id = cursor.getInt(0);
            if (id == Consts.BENGALI_TAJWEED_TRANSLITERATION_ID)
                quranBangla = ResourcesCompat.getFont(this, R.font.banglaquran);
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + id + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor sqLiteCursor = database.rawQuery("select text from content", null);
            transData[i] = new TransData(id, name, sqLiteCursor);
        }
        cursor.close();
        sqLiteDatabase.close();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.print, menu);
        inf.inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.print) {
            dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.loading)
                    .setTitle("creating PDF")
                    .setNegativeButton(R.string.cancel, (d, which) -> {
                        task.cancel();
                        dialog = new AlertDialog.Builder(PrintActivity.this)
                                .setView(R.layout.loading).show();
                    }).show();
            task = new PdfCreator(fullQuranAdapter, this);
            task.start();
        } else if (id == R.id.setting) startActivity(new Intent(this, SettingActivity.class));
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (RecreateManager.needRecreate(RecreateManager.QURAN_ACTIVITY)) recreate();
    }

    void initData() {
        final SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        Utils.dataPath = store.getString(Consts.PATH_KEY, null);
        String[] names = getResources().getStringArray(R.array.sura_transliteration);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = db.rawQuery("select rowid,text,word from quran where ayah is null", null);
        suraInfo = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext())
            suraInfo.add(new SuraInfo(cursor.getInt(1), cursor.getInt(2), names[cursor.getPosition()]));
        cursor.close();
        db.close();
        float density = getResources().getDisplayMetrics().scaledDensity;
        boolean showByWord = store.getBoolean(Consts.SHOW_BY_WORD, true);
        boolean showTrans = store.getBoolean(Consts.SHOW_TRANS, true);
        if (showTrans) {
            getTrans();
            justifyTrans = store.getBoolean(Consts.JUSTIFICATION, false);
        }
        if (showByWord) getWord();
        banglaFontSize = (store.getInt(Consts.FONT_KEY, Consts.DEF_FONT) - 6) * density;
        arabicFontSize = (store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) - 8) * density;
        int fontId = store.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0) arabicFont = ResourcesCompat.getFont(this, Consts.FONT_LIST[fontId]);
        kalpurush = ResourcesCompat.getFont(this, R.font.kalpurush);
        boolean showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        boolean showTajweed = store.getBoolean(Consts.TAJWEED_KEY, true);
        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.tab_text, R.attr.inactive});
        tabTextColor = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else typedArray.recycle();
        if (showArabic) {
            viewPool = new Stack<>();
        }
        if (showArabic && showTajweed) {
            ghunna = Pattern.compile("[\u0646\u0645]\u0651.\u0670?").matcher("");
            iqlab = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u06e2\u06ed].?.? *\u0628\u0651?.\u0670?\u0653?").matcher("");
            qalqala = Pattern.compile("[\u0642\u0637\u0628\u062c\u062f](\u0652|[^اىو]{0,2}$)").matcher("");
            idgham = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u0652\u06e1\u0627\u0649\u06db\u06da\u06d7\u06d6\u06d9]* +" + "[\u0646\u0645\u064a\u0648]\u0651?.\u0670?\u0653?|\u0645[\u06db\u06da\u06d7\u06d6\u06d9\u0652\u06e1]* +\u0645\u0651?.\u0670?\u0653?").matcher("");
            ikhfa = Pattern.compile("(\u0646\u0652?|.\u0651?[\u064d\u064b\u064c])[\u06e1\u0627\u0649\u06db\u06da\u06d7\u06d6\u06d9 ]*" + "[\u0635\u0630\u062b\u0643\u062c\u0634\u0642\u0633\u062f\u0637\u0632\u0641\u062a\u0636\u0638\u06a9]\u0651?.\u0670?\u0653?" + "|\u0645[\u0652\u06e1 ]*\u0628\u0651?.\u0670?\u0653?").matcher("");
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("ATTACH DATABASE ? AS q", new String[]{Utils.dataPath + Consts.QURAN_DB_NAME});
        String query = "SELECT quran.sura,quran.ayah,quran.";
        if (fontId >= 5) query += "indo,";
        else query += "text,";
        mainCursor = database.rawQuery(query + "juz,bk.sura,word " + "from q.quran left outer join (select distinct sura,ayah from bookmark where category<1000000000) as bk " + "using (sura,ayah)", null);
        new Handler(Looper.getMainLooper()).post(this::initView);
    }

    void initView() {
        dialog.dismiss();
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        RecyclerView recyclerView = drawerLayout.findViewById(R.id.main_list);
        drawerLayout.findViewById(R.id.controller).setVisibility(View.GONE);
        ViewPager2 viewPager2 = drawerLayout.findViewById(R.id.view_pager2);
        drawerLayout.removeView(viewPager2);
        LinearLayoutManager layoutManager = new MyLayoutManager(this);
        fullQuranAdapter = new PrintQuranAdapter(this);
        recyclerView.setAdapter(fullQuranAdapter);
        recyclerView.setLayoutManager(layoutManager);
        Toolbar toolbar = drawerLayout.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    boolean withoutArabic() {
        return viewPool == null;
    }


    boolean justifyTrans() {
        return justifyTrans;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Codes.WRITE_REQUEST_CODE && data != null) {
            try {
                assert data.getData() != null;
                Utils.copy(new File(getCacheDir(), "quran.pdf"), getContentResolver().openOutputStream(data.getData()));
            } catch (Exception e) {
                L.d(e);
            }
        }
    }

    @Override
    public void onClick(DialogInterface d, int which) {
        String mimeType = "application/pdf";
        Intent intent;
        File file = new File(getCacheDir(), "quran.pdf");
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                startActivity(new Intent(this, PDFActivity.class).setData(Uri.fromFile(file)));
                return;
            case DialogInterface.BUTTON_POSITIVE:
                intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, "quran.pdf");
                startActivityForResult(intent, Codes.WRITE_REQUEST_CODE);
                return;
            case DialogInterface.BUTTON_NEUTRAL:
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider", file));
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onFinish() {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            L.d(e);
        }
        new AlertDialog.Builder(this)
                .setTitle("PDF created successfully")
                .setNeutralButton("Share file", this)
                .setPositiveButton("Save file", this)
                .setNegativeButton("open file", this)
                .show();
    }

    @Override
    public void onProgress(int progress) {
        dialog.setMessage("Progress: " + progress + "/" + fullQuranAdapter.getItemCount());
    }

    static class SuraInfo {
        final int totalAyah;
        final boolean makki;
        final String name;

        SuraInfo(int totalAyah, int word, String name) {
            this.totalAyah = totalAyah;
            this.makki = word == 0;
            this.name = name;
        }
    }
}
