package bn.poro.quran.book_section;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.my_paging.PagerDataModel;
import bn.poro.quran.my_paging.PagerDataProvider;
import bn.poro.quran.views.FontSpan;

class MainTextAdapter extends RecyclerView.Adapter<MainTextAdapter.MyHolder>
        implements PagerDataProvider<MainTextAdapter.BookItemModel>, DialogInterface.OnClickListener {
    private final int arabicFontSize, fontSize;
    int scrollOffset, resultAtPos, resultOffset;
    private final String bookID;
    final Activity activity;
    private final PagerDataModel<BookItemModel> dataModel;
    private final boolean showArabic;
    private Matcher matcher;
    private final Typeface arabicFont;
    final LinearLayoutManager layoutManager;
    private int itemCount;
    private String headline;
    private StringBuilder text;

    MainTextAdapter(Activity activity, LinearLayoutManager layoutManager, String bookID) {
        this.activity = activity;
        this.bookID = bookID;
        this.layoutManager = layoutManager;
        dataModel = new PagerDataModel<>(this, layoutManager, this);
        arabicFont = ResourcesCompat.getFont(activity, R.font.hafs);
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        fontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT);
        if (showArabic) {
            matcher = Pattern.compile(Consts.ARABIC_MATCHER).matcher("");
        }
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id;
        if (viewType == 1) id = R.layout.book_section;
        else if (viewType == 0) id = R.layout.textview;
        else id = R.layout.place_holder;
        return new MyHolder(activity.getLayoutInflater().inflate(id, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        BookItemModel itemModel = dataModel.getItem(position);
        holder.itemView.setTag(position);
        int id = holder.itemView.getId();
        if (itemModel == null || id == R.id.place_holder) return;
        String string = itemModel.text;//replace("\u00a0", " ");
        SpannableStringBuilder spannableString;
        if (showArabic) {
            string = string
                    .replaceAll("ہ", "ه")
                    .replaceAll("ی", "ي")
                    .replaceAll("ک", "ك")
                    .replaceAll("ۃ", "ة")
                    .replaceAll("﴿[^﴾ ]*﴾?", "\n");
            spannableString = new SpannableStringBuilder(string);
            matcher.reset(spannableString);
            while (matcher.find()) {
                spannableString.setSpan(new FontSpan(arabicFont), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new AbsoluteSizeSpan(arabicFontSize, true),
                        matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            string = string.replaceAll(Consts.ARABIC_MATCHER, "");
            spannableString = new SpannableStringBuilder(string);
        }

        if (activity instanceof SearchBookActivity) {
            SearchBookActivity searchBook = (SearchBookActivity) activity;
            int start = 0;
            int offset = 0;
            int len = searchBook.searchText.length();
            while ((start = string.indexOf(searchBook.searchText, start)) != -1) {
                int end = start + len;
                int color;
                if (position == this.resultAtPos && offset == this.resultOffset) {
                    scrollOffset = start;
                    new Handler(Looper.getMainLooper()).post(new SearchResultTraversal());
                    color = 0xAAFF1744;
                } else color = 0xffF9A825;
                spannableString.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end;
                offset++;
            }
        } else if (resultAtPos == position && scrollOffset != 0)
            new Handler(Looper.getMainLooper()).post(() -> {
                layoutManager.scrollToPositionWithOffset(resultAtPos, scrollOffset);
                resultAtPos = -1;
                scrollOffset = 0;
            });

        if (id == R.id.text) {
            Matcher matches = Pattern.compile("(\\[\\d+?])[^\u00A0]*?\u00A0").matcher(string);
            int deleted = 0;
            while (matches.find()) {
                int start = matches.start(1) - deleted;
                int end = matches.end(1) - deleted;
                spannableString.setSpan(new SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new RelativeSizeSpan(0.75f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new ClickSpan(matches.group(0)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = matches.end(0) - deleted;
                if (start > spannableString.length()) {
                    start = spannableString.length();
                }
                spannableString.delete(end, start);
                deleted += start - end;
            }
        }
        holder.textView.setText(spannableString);
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    @Override
    public int getItemViewType(int position) {
        BookItemModel itemModel = dataModel.getItem(position);
        if (itemModel == null) return 2;
        if (itemModel.type == 0) return 0;
        return 1;
    }

    @Override
    public List<BookItemModel> fetch(int request) {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOK_SUB_PATH
                + this.bookID, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        int start = request * PagerDataModel.PAGE_SIZE;
        int end = (request + 1) * PagerDataModel.PAGE_SIZE - 1;
        Cursor cursor = database.rawQuery("SELECT text,type from content where id between ? and ?", new String[]{String.valueOf(start), String.valueOf(end)});
        List<BookItemModel> itemModels = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext())
            itemModels.add(new BookItemModel(cursor.getString(0), cursor.getInt(1)));
        cursor.close();
        database.close();
        return itemModels;
    }

    @Override
    public void init() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOK_SUB_PATH
                + this.bookID, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("SELECT count(1) from content;", null);
        cursor.moveToFirst();
        itemCount = cursor.getInt(0);
        cursor.close();
        database.close();
    }

    @Override
    public void onClick(DialogInterface dialog, int id) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(headline);
        stringBuilder.append("\n\n");
        stringBuilder.append(text);
        stringBuilder.append("বইঃ ");
        stringBuilder.append(activity.getTitle());
        switch (id) {
            case 0:
                View view = layoutManager.getChildAt(0);
                if (view == null) return;
                SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                database.execSQL("insert into book_mark values(?,?,?,?,?);", new Object[]{bookID,
                        view.getTag(), view.getTop(), System.currentTimeMillis(), headline});
                database.close();
                Toast.makeText(activity, "Bookmark added", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("label", stringBuilder));
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        Toast.makeText(activity, headline + " কপি হয়েছে", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
                activity.startActivity(intent);
                break;
        }
    }

    class MyHolder extends RecyclerView.ViewHolder implements
            View.OnLongClickListener, Runnable {
        TextView textView;

        MyHolder(@NonNull View view) {
            super(view);
            if (view.getId() == R.id.place_holder) return;
            textView = (TextView) view;
            if (view.getId() == R.id.section) {
                textView.setTextSize(fontSize + 2);
            } else textView.setTextSize(fontSize);
            textView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            MenuAdapter adapter = new MenuAdapter(activity);
            new AlertDialog.Builder(activity).setAdapter(adapter, MainTextAdapter.this).show();
            new Thread(this).start();
            return false;
        }


        @Override
        public void run() {
            int position = getLayoutPosition();
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOK_SUB_PATH
                    + bookID, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select id,text from content where id<=? and type is not null order by id desc limit 1", new String[]{String.valueOf(position)});
            if (cursor.moveToFirst()) {
                position = cursor.getInt(0);
                headline = cursor.getString(1);
            } else {
                position = -1;
                headline = "শুরুর কথা";

            }
            cursor.close();
            cursor = database.rawQuery("select text,type from content where id>" + position, null);
            text = new StringBuilder();
            while (cursor.moveToNext()) {
                if (cursor.getString(1) != null) break;
                text.append(cursor.getString(0)).append("\n\n");
            }
            cursor.close();
            database.close();
        }
    }

    public static class BookItemModel {
        final String text;
        final int type;

        public BookItemModel(String text, int type) {
            this.text = text;
            this.type = type;
        }
    }

    private class SearchResultTraversal implements Runnable {
        @Override
        public void run() {
            if (scrollOffset < 0) {
                layoutManager.scrollToPosition(resultAtPos);
                return;
            }
            View topChild = layoutManager.getChildAt(0);
            if (topChild == null) return;
            topChild = layoutManager.getChildAt(resultAtPos - layoutManager.getPosition(topChild));
            if (topChild == null) return;
            TextView textView = (TextView) topChild;
            Layout layout = textView.getLayout();
            if (layout == null) return;
            int line = layout.getLineForOffset(scrollOffset);
            int lineTop = layout.getLineTop(line);
            int lineBottom = layout.getLineBottom(line);
            View parent = (View) topChild.getParent();
            int visibleHeight = parent.getHeight() - parent.getTop();
            int viewTop = -(textView.getTop() + topChild.getTop() + textView.getPaddingTop());
            if (lineTop < viewTop || lineBottom > viewTop + visibleHeight) {
                layoutManager.scrollToPositionWithOffset(resultAtPos, (visibleHeight / 2) - lineTop);
            }
        }
    }
}
