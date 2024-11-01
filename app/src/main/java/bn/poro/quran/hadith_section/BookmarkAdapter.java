package bn.poro.quran.hadith_section;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.views.FontSpan;

class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.MyHolder> {
    private final int fontSize, arabicFontSize;
    private final Context context;
    private final ArrayList<BookmarkItem> bookmarkItems;
    private final boolean showArabic, showHaraka;
    private final Typeface arabicFont;

    BookmarkAdapter(@NonNull Context context) {
        this.context = context;
        SharedPreferences store = context.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        fontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        arabicFont = ResourcesCompat.getFont(context, R.font.hafs);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT);
        showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        showHaraka = store.getBoolean(Consts.SHOW_HARAKA_KEY, true);
        bookmarkItems = new ArrayList<>();
    }

    void refresh() {
        bookmarkItems.clear();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("attach database ? as bookmarks", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
        Cursor cursor = database.rawQuery("select book,position,num,time,name from hadis_mark left outer join hadis on book=id", null);
        while (cursor.moveToNext()) {
            bookmarkItems.add(new BookmarkItem(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getInt(3), cursor.getString(4)));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(context).inflate(R.layout.fav_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        BookmarkItem item = bookmarkItems.get(position);
        holder.name.setText(item.name + "-" + Utils.formatNum(item.num));
        holder.hadis.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return bookmarkItems.size();
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, DialogInterface.OnClickListener {
        final TextView hadis;
        final TextView name;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            name = itemView.findViewById(R.id.num);
            hadis = itemView.findViewById(R.id.hadis);
            hadis.setTextSize(fontSize);
            name.setTextSize(fontSize);
            name.setOnClickListener(this);
            itemView.findViewById(R.id.bookmark).setOnClickListener(this);
            itemView.findViewById(R.id.done).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            BookmarkItem item = bookmarkItems.get(getLayoutPosition());
            if (id == R.id.bookmark) {
                new AlertDialog.Builder(context)
                        .setTitle("ফেভারিট থেকে মুছে ফেলবেন?")
                        .setPositiveButton(R.string.delete, this)
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else if (id == R.id.done) {
                context.startActivity(new Intent(context, ReadHadisActivity.class)
                        .putExtra(Consts.ID_KEY, item.book).putExtra(Consts.TITLE_KEY,item.name)
                        .putExtra(Consts.HADITH_NO, item.position));
            } else if (id == R.id.num) {
                if (hadis.getVisibility() == View.VISIBLE) {
                    hadis.setVisibility(View.GONE);
                    return;
                }
                SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.HADIS_SUB_PATH + item.book + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                Cursor cursor = database.rawQuery("select bn,ar from content where num=" + item.num, null);
                if (cursor.moveToFirst()) {
                    SpannableString spannedArabic = null;
                    if (showArabic) {
                        String arabic = cursor.getString(1);
                        if (arabic != null) {
                            arabic.replaceAll("،", "⹁");
                            if (!showHaraka) arabic = arabic.replaceAll(Consts.HARAKA_REGEX, "");
                            spannedArabic = new SpannableString(arabic);
                            spannedArabic.setSpan(new FontSpan(arabicFont), 0, spannedArabic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spannedArabic.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), 0, spannedArabic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                    if (spannedArabic != null) {
                        stringBuilder.append(spannedArabic);
                        stringBuilder.append("\n\n");
                    }
                    String bangla = cursor.getString(0);
                    if (showArabic) {
                        if (!showHaraka) bangla = bangla.replaceAll(Consts.HARAKA_REGEX, "");
                        SpannableString spannableString = new SpannableString(bangla.replaceAll("،", "⹁"));
                        Matcher matcher = Pattern.compile(Consts.ARABIC_MATCHER).matcher(spannableString);
                        while (matcher.find()) {
                            spannableString.setSpan(new FontSpan(arabicFont), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spannableString.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        stringBuilder.append(spannableString);
                    } else
                        stringBuilder.append(bangla.replaceAll(Consts.ARABIC_MATCHER, ""));
                    hadis.setText(stringBuilder);
                    hadis.setVisibility(View.VISIBLE);
                } else L.d("empty cursor");
                cursor.close();
                database.close();
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = getLayoutPosition();
            BookmarkItem item = bookmarkItems.remove(position);
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("delete from hadis_mark where book=? and num=?", new Integer[]{item.book, item.num});
            notifyItemRemoved(position);
            database.close();
        }
    }
}
