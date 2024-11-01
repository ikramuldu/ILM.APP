package bn.poro.quran.hadith_section;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.my_paging.PagerDataModel;
import bn.poro.quran.my_paging.PagerDataProvider;
import bn.poro.quran.views.FontSpan;

class HadisTextAdapter extends RecyclerView.Adapter<HadisTextAdapter.MyHolder> implements PagerDataProvider<HadisTextAdapter.HadisData> {

    private static final int TYPE_HADIS_HEAD = 0;
    private static final int TYPE_PLACEHOLDER = 2;
    private static final int TYPE_HADIS = 1;
    private final int fontSize, arabicFontSize;
    private final ReadHadisActivity activity;
    private final boolean justification;
    private Matcher matcher;
    final boolean showArabic;
    final boolean showHaraka;
    private final Typeface arabicFont;
    private final PagerDataModel<HadisData> dataModel;
    private final ArrayList<Integer> bookmarks;
    private int itemCount;

    HadisTextAdapter(ReadHadisActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        dataModel = new PagerDataModel<>(this, layoutManager, this);
        bookmarks = new ArrayList<>();
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        fontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT);
        showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        showHaraka = store.getBoolean(Consts.SHOW_HARAKA_KEY, true);
        justification = store.getBoolean(Consts.JUSTIFICATION, false);
        arabicFont = ResourcesCompat.getFont(activity, R.font.hafs);
        if (showArabic)
            matcher = Pattern.compile(Consts.ARABIC_MATCHER).matcher("");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == TYPE_HADIS_HEAD) layout = R.layout.hadis_head;
        else if (viewType == TYPE_PLACEHOLDER) layout = R.layout.place_holder;
        else layout = R.layout.hadis;
        return new MyHolder(activity.getLayoutInflater().inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        HadisData hadisData = dataModel.getItem(position);
        if (hadisData == null) return;
        String bangla = hadisData.bn;
        SpannableString spannedArabic = null;
        if (showArabic) {
            String arabic = hadisData.ar;
            if (arabic != null) {
                arabic = arabic.replaceAll("،", "⹁")
                        .replaceAll("ہ", "ه")
                        .replaceAll("ی", "ي")
                        .replaceAll("ک", "ك")
                        .replaceAll("ۃ", "ة");
                if (!showHaraka) arabic = arabic.replaceAll(Consts.HARAKA_REGEX, "");
                spannedArabic = new SpannableString(arabic);
                spannedArabic.setSpan(new FontSpan(arabicFont), 0, spannedArabic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannedArabic.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), 0, spannedArabic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (holder.hadisNumber == null) {
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(bangla);
            if (hadisData.type == 1) {
                if (spannedArabic != null) {
                    stringBuilder.append(" (");
                    stringBuilder.append(spannedArabic);
                    stringBuilder.append(')');
                }
                stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (spannedArabic != null) {
                stringBuilder.append('\n');
                stringBuilder.append(spannedArabic);
            }
            holder.textView.setText(stringBuilder);
            return;
        }
        holder.hadisNumber.setText(activity.getString(R.string.hadis_no, hadisData.num));
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (spannedArabic != null) {
            stringBuilder.append(spannedArabic);
            stringBuilder.append("\n\n");
        }
        if (showArabic) {
            if (!showHaraka) bangla = bangla.replaceAll(Consts.HARAKA_REGEX, "");
            SpannableString spannableString = new SpannableString(bangla.replaceAll("،", "⹁"));
            matcher.reset(spannableString);
            while (matcher.find()) {
                spannableString.setSpan(new FontSpan(arabicFont), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            stringBuilder.append(spannableString);
        } else
            stringBuilder.append(bangla.replaceAll(Consts.ARABIC_MATCHER, ""));
        holder.textView.setText(stringBuilder);
        if (bookmarks.contains(hadisData.num)) {
            holder.bookmark.setImageResource(R.drawable.ic_bookmark_on);
        } else {
            holder.bookmark.setImageResource(R.drawable.ic_bookmark);
        }
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        HadisData data = dataModel.getItem(position);
        if (data == null) return TYPE_PLACEHOLDER;
        return data.type == 0 ? TYPE_HADIS : TYPE_HADIS_HEAD;
    }

    @Override
    public List<HadisData> fetch(int request) {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath
                + Consts.HADIS_SUB_PATH + activity.bookID + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        int start = (request * PagerDataModel.PAGE_SIZE);
        int end = ((request + 1) * PagerDataModel.PAGE_SIZE) - 1;
        Cursor cursor = database.rawQuery("select bn,type,ar,num from content where rowid between ? and ?", new String[]{String.valueOf(start), String.valueOf(end)});
        List<HadisData> data = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            String bn = cursor.getString(0);
            int type = cursor.getInt(1);
            String ar = cursor.getString(2);
            int num = cursor.getInt(3);
            data.add(new HadisData(bn, type, ar, num));
        }
        cursor.close();
        return data;
    }

    @Override
    public void init() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath
                + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select num from hadis_mark", null);
        while (cursor.moveToNext()) bookmarks.add(cursor.getInt(0));
        cursor.close();
        database.close();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.HADIS_SUB_PATH + activity.bookID + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor1 = db.rawQuery("select count(1) from content", null);
        cursor1.moveToFirst();
        itemCount = cursor1.getInt(0);
        cursor1.close();
        db.close();
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView, hadisNumber;
        ImageView bookmark;

        @SuppressLint("WrongConstant")
        MyHolder(@NonNull View view) {
            super(view);
            int id = view.getId();
            if (id == R.id.place_holder) return;
            if (id == R.id.header) {
                textView = view.findViewById(R.id.text);
                textView.setTextSize(fontSize + 2);
                return;
            }
            bookmark = view.findViewById(R.id.bookmark);
            bookmark.setOnClickListener(this);
            view.findViewById(R.id.copy_text).setOnClickListener(this);
            view.findViewById(R.id.share_text).setOnClickListener(this);
            textView = view.findViewById(R.id.hadis);
            if (justification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                textView.setTextIsSelectable(false);
                textView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
            textView.setTextSize(fontSize);
            hadisNumber = view.findViewById(R.id.num);
            hadisNumber.setTextSize(fontSize);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            HadisData hadisData = dataModel.getItem(position);
            if (hadisData == null) return;
            SQLiteDatabase database;
            int id = v.getId();
            if (id == R.id.bookmark) {
                database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null,
                        SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                if (!bookmarks.contains(hadisData.num)) {
                    bookmark.setImageResource(R.drawable.ic_bookmark_on);
                    database.execSQL("insert into hadis_mark values(?,?,?,?)", new Object[]{
                            activity.bookID, position, hadisData.num, System.currentTimeMillis()
                    });
                    bookmarks.add(hadisData.num);
                    Toast.makeText(activity, "Bookmark added", Toast.LENGTH_SHORT).show();
                } else {
                    bookmark.setImageResource(R.drawable.ic_bookmark);
                    database.execSQL("delete from hadis_mark where book=? and num=?", new Integer[]{activity.bookID, hadisData.num});
                    bookmarks.remove((Integer) hadisData.num);
                    Toast.makeText(activity, "Bookmark removed", Toast.LENGTH_SHORT).show();
                }
                database.close();
            } else if (id == R.id.copy_text) {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                assert clipboard != null;
                clipboard.setPrimaryClip(ClipData.newPlainText("label", textView.getText()));
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(activity, String.format(Locale.US, "%s নং হাদীস কপি হয়েছে", hadisData.num), Toast.LENGTH_SHORT).show();
            } else if (id == R.id.share_text) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
                activity.startActivity(intent);
            }
        }
    }

    static class HadisData {
        private final String bn, ar;
        private final int type, num;

        public HadisData(String bn, int type, String ar, int num) {
            this.bn = bn;
            this.type = type;
            this.ar = ar;
            this.num = num;
        }
    }
}

