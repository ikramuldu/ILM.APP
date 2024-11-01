package bn.poro.quran.hadith_section;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.views.FontSpan;

class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> {
    private static final int TYPE_HADIS = 1;
    private static final int TYPE_HEAD = 0;
    private final SearchHadithActivity activity;
    private final Cursor cursor;
    private final int fontSize, arabicFontSize;
    private final int bookId;
    private Matcher matcher;
    private final boolean showArabic;
    private final boolean showHaraka;
    private final Typeface arabicFont;
    private final String bookTitle;

    SearchAdapter(SearchHadithActivity activity, Bundle arguments) {
        this.activity = activity;
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        fontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        arabicFontSize = store.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT);
        showArabic = store.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        showHaraka = store.getBoolean(Consts.SHOW_HARAKA_KEY, true);
        arabicFont = ResourcesCompat.getFont(activity, R.font.hafs);
        bookId = arguments.getInt(HadisResult.ID_KEY);
        bookTitle = arguments.getString(HadisResult.NAME_KEY);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.HADIS_SUB_PATH + bookId + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        cursor = database.rawQuery("select id,type,bn,ar,num from content where id in (" + arguments.getString(HadisResult.RESULT_KEY) + ")", null);
        if (showArabic) matcher = Pattern.compile(Consts.ARABIC_MATCHER).matcher("");
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(viewType == TYPE_HADIS ? R.layout.src_item : R.layout.src_hadis_head, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        cursor.moveToPosition(position);
        if (cursor.getString(1) == null) return TYPE_HADIS;
        return TYPE_HEAD;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        cursor.moveToPosition(position);
        holder.itemView.setTag(position);
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        String arabic = cursor.getString(3);
        if (showArabic && arabic != null) {
            arabic = arabic.replaceAll("،", "⹁");
            if (!showHaraka) arabic = arabic.replaceAll(Consts.HARAKA_REGEX, "");
            stringBuilder.append(arabic);
            stringBuilder.append('\n');
            stringBuilder.setSpan(new FontSpan(arabicFont), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.append('\n');
        }
        String bangla = cursor.getString(2);
        if (showArabic) {
            if (!showHaraka) bangla = bangla.replaceAll(Consts.HARAKA_REGEX, "");
            bangla = bangla.replaceAll("،", "⹁");

            SpannableString spannableString = new SpannableString(bangla);
            matcher.reset(spannableString);
            while (matcher.find()) {
                spannableString.setSpan(new FontSpan(arabicFont), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new AbsoluteSizeSpan(arabicFontSize, true), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            stringBuilder.append(spannableString);
        } else
            stringBuilder.append(bangla.replaceAll(Consts.ARABIC_MATCHER, ""));
        String search = activity.getSearchString();
        if (showArabic && Pattern.compile("[\u0600-\u06ff]").matcher(search).find()) {
            if (showHaraka) {
                StringBuilder regex = createRegex(search);
                Matcher matcher = Pattern.compile(regex.toString()).matcher(stringBuilder);
                while (matcher.find()) {
                    stringBuilder.setSpan(new ForegroundColorSpan(0xffF9A825), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else search = search.replaceAll(Consts.HARAKA_REGEX, "");
        }

        bangla = stringBuilder.toString();
        int start = 0;
        while ((start = bangla.indexOf(search, start)) != -1) {
            stringBuilder.setSpan(new ForegroundColorSpan(0xffFF1744), start, start + search.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start += search.length();
        }
        if (holder.num != null) {
            holder.num.setText(activity.getString(R.string.hadis_no, cursor.getInt(4)));
        } else {
            Drawable drawable = ActivityCompat.getDrawable(activity, R.drawable.ic_go);
            if (drawable != null) {
                start = stringBuilder.length() + 1;
                stringBuilder.append(" x");
                drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * 0.7), (int) (drawable.getIntrinsicHeight() * 0.7));
                stringBuilder.setSpan(new ImageSpan(drawable, DynamicDrawableSpan.ALIGN_BOTTOM), start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        holder.textView.setText(stringBuilder);
    }

    private static StringBuilder createRegex(String search) {
        char[] chars = search.replaceAll(Consts.HARAKA_REGEX, "").toCharArray();
        StringBuilder regex = new StringBuilder();
        for (char c : chars) {
            if (c >= '\u0600' && c <= 'ۿ') {
                regex.append(c);
                regex.append("[\u064B-\u0670]*");
            } else {
                if (".+*?^$()[]{}|\\".indexOf(c) != -1) {
                    regex.append("\\");
                }
                regex.append(c);
            }
        }
        return regex;
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    Cursor getCursor() {
        return cursor;
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        TextView num;

        public Holder(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.hadis);
            textView.setTextSize(fontSize);
            if (view.getId() == R.id.header) return;
            view.findViewById(R.id.copy_text).setOnClickListener(this);
            view.findViewById(R.id.done).setOnClickListener(this);
            view.findViewById(R.id.share_text).setOnClickListener(this);
            num = view.findViewById(R.id.num);
            num.setTextSize(fontSize + 3);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.copy_text) {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                assert clipboard != null;
                CharSequence label = num.getText();
                clipboard.setPrimaryClip(ClipData.newPlainText(label, textView.getText()));
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                    Toast.makeText(activity, label + " কপি হয়েছে", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.share_text) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
                activity.startActivity(intent);
            } else if (id == R.id.done) {
                cursor.moveToPosition(getLayoutPosition());
                activity.startActivity(new Intent(activity, ReadHadisActivity.class)
                        .putExtra(Consts.ID_KEY, bookId).putExtra(Consts.TITLE_KEY, bookTitle).putExtra(Consts.HADITH_NO, cursor.getInt(0)));
            }
        }
    }
}
