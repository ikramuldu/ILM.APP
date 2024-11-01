package bn.poro.quran.print;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.CompressTask;
import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.views.WordGroup;

public class PrintQuranAdapter extends RecyclerView.Adapter<PrintQuranAdapter.Holder> implements CompressTask.CompressListener {
    private static final int TYPE_SURA = 0;
    private static final int TYPE_JUZ = 2;
    private static final int TYPE_AYAH = 1;
    public static final int SURA_INDEX = 0;
    public static final int AYAH_INDEX = 1;
    public static final int ARABIC_INDEX = 2;
    private static final int JUZ_INDEX = 3;
    private static final int WORD_INDEX = 5;
    final PrintActivity activity;

    PrintQuranAdapter(PrintActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        switch (viewType) {
            case TYPE_SURA:
                layout = R.layout.only_head;
                break;
            case TYPE_JUZ:
                layout = R.layout.with_head_print;
                break;
            default:
                layout = R.layout.item_ayah_print;
        }
        LayoutInflater inflater = LayoutInflater.from(activity);
        ViewGroup view = (ViewGroup) inflater.inflate(layout, parent, false);
        if (activity.transData != null) inflater.inflate(R.layout.print_trans, view);
        return new Holder(view);
    }

    @Override
    public int getItemViewType(int position) {
        activity.mainCursor.moveToPosition(position);
        if (activity.mainCursor.getInt(AYAH_INDEX) == 0) return TYPE_SURA;
        if (activity.mainCursor.getInt(JUZ_INDEX) != 0) return TYPE_JUZ;
        return TYPE_AYAH;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        activity.mainCursor.moveToPosition(position);
        if (activity.transData != null) {
            holder.transText.setTag(position + 1);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            TransData[] transData = activity.transData;
            for (TransData data : transData) {
                data.cursor.moveToPosition(position);
                String string = data.cursor.getString(0);
                if (string == null) continue;
                if (stringBuilder.length() > 0)
                    stringBuilder.append("\n\n");
                int start = stringBuilder.length();
                String name = data.name;
                int end;
                if (activity.transData.length > 1) {
                    stringBuilder.append(name);
                    end = stringBuilder.length();
                    stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    stringBuilder.setSpan(new ForegroundColorSpan(activity.tabTextColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    stringBuilder.append("\n");
                }
                String br;
                if (Utils.notContains(Consts.RTL, name.substring(0, name.indexOf('-')))) {
                    stringBuilder.append("\u200E");
                    br = "<br>\u200E";
                } else br = "<br>";
                start = stringBuilder.length();
                stringBuilder.append(Html.fromHtml(string.replaceAll("\n", br)));
                end = stringBuilder.length();
                if (name.startsWith("Bengali")) {
                    if (data.id == Consts.BENGALI_TAJWEED_TRANSLITERATION_ID)
                        stringBuilder.setSpan(new FontSpan(activity.quranBangla), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    else
                        stringBuilder.setSpan(new FontSpan(activity.kalpurush), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

            }
            if (stringBuilder.length() == 0) {
                holder.transText.setVisibility(View.GONE);
            } else {
                holder.transText.setVisibility(View.VISIBLE);
                holder.transText.setText(stringBuilder);
            }
        }
        int sura = activity.mainCursor.getInt(SURA_INDEX);
        if (holder.headerText != null) {
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            if (activity.mainCursor.getInt(JUZ_INDEX) != 0) {
                stringBuilder.append(activity.getString(R.string.quran_juz2));
                stringBuilder.append(": ");
                stringBuilder.append(String.valueOf(Utils.formatNum(activity.mainCursor.getInt(JUZ_INDEX))));
                stringBuilder.setSpan(new RelativeSizeSpan(0.85f), 0,
                        stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (activity.mainCursor.getInt(AYAH_INDEX) == 0) stringBuilder.append("\n");
            }
            if (activity.mainCursor.getInt(AYAH_INDEX) == 0) {
                stringBuilder.append(Utils.formatNum(sura + 1));
                stringBuilder.append(". ");
                stringBuilder.append(activity.suraInfo.get(sura).name);
                stringBuilder.append("\n");
                int start = stringBuilder.length();
                stringBuilder.append(activity.getString(R.string.sura_num_ayahs));
                stringBuilder.append(' ');
                stringBuilder.append(Utils.formatNum(activity.suraInfo.get(sura).totalAyah));
                if (activity.suraInfo.get(sura).makki) {
                    stringBuilder.append(", ");
                    stringBuilder.append(activity.getString(R.string.makki));
                } else {
                    stringBuilder.append(", ");
                    stringBuilder.append(activity.getString(R.string.madani));
                }
                stringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                //stringBuilder.append("\n");
                if (sura != 0 && sura != 8) {
                    stringBuilder.append("\n");
                    start = stringBuilder.length();
                    stringBuilder.append(activity.getString(R.string.bismillah));
                    if (activity.arabicFont != null)
                        stringBuilder.setSpan(new FontSpan(activity.arabicFont), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            holder.headerText.setText(stringBuilder);
        }
        if (holder.ayahNo == null) return;
        holder.ayahNo.setText(activity.suraInfo.get(sura).name + "-" + Utils.formatNum(activity.mainCursor.getInt(AYAH_INDEX)));
        if (activity.withoutArabic()) return;
        int viewCount = holder.wordGroup.getChildCount();
        LayoutInflater inflater = activity.getLayoutInflater();
        String arabicText = activity.mainCursor.getString(ARABIC_INDEX);
        String[] arabicWords = arabicText.split(" ");
        int len = arabicText.length();
        SpannableString fullAyahSpannable = new SpannableString(arabicText);
        if (activity.arabicFont != null)
            fullAyahSpannable.setSpan(new FontSpan(activity.arabicFont), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        fullAyahSpannable.setSpan(new AbsoluteSizeSpan((int) activity.arabicFontSize, false), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (activity.qalqala != null) {
            activity.qalqala.reset(fullAyahSpannable);
            while (activity.qalqala.find()) {
                fullAyahSpannable.setSpan(new ForegroundColorSpan(Consts.tajweed[0]), activity.qalqala.start(), activity.qalqala.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            List<Integer> starts = new ArrayList<>();
            activity.iqlab.reset(fullAyahSpannable);
            while (activity.iqlab.find()) {
                starts.add(activity.iqlab.start());
                fullAyahSpannable.setSpan(new ForegroundColorSpan(Consts.tajweed[1]), activity.iqlab.start(), activity.iqlab.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            List<Integer> ends = new ArrayList<>();
            activity.idgham.reset(fullAyahSpannable);
            while (activity.idgham.find()) {
                starts.add(activity.idgham.start());
                ends.add(activity.idgham.end());
                fullAyahSpannable.setSpan(new ForegroundColorSpan(Consts.tajweed[3]), activity.idgham.start(), activity.idgham.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            activity.ikhfa.reset(fullAyahSpannable);
            while (activity.ikhfa.find()) {
                starts.add(activity.ikhfa.start());
                fullAyahSpannable.setSpan(new ForegroundColorSpan(Consts.tajweed[4]), activity.ikhfa.start(), activity.ikhfa.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            activity.ghunna.reset(fullAyahSpannable);
            while (activity.ghunna.find()) {
                if (starts.contains(activity.ghunna.start()) || ends.contains(activity.ghunna.end()))
                    continue;
                fullAyahSpannable.setSpan(new ForegroundColorSpan(Consts.tajweed[5]), activity.ghunna.start(), activity.ghunna.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (arabicText.endsWith("۩")) {
            fullAyahSpannable.setSpan(new ForegroundColorSpan(0xff81d4fa), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int start = 0;
        int wordId = activity.mainCursor.getInt(WORD_INDEX) - 1;
        for (int i = 0; i < arabicWords.length; i++) {
            TextView textView;
            if (i < viewCount) textView = (TextView) holder.wordGroup.getChildAt(i);
            else {
                if (activity.viewPool.isEmpty()) {
                    textView = (TextView) inflater.inflate(R.layout.word_print, holder.wordGroup, false);
                } else {
                    textView = activity.viewPool.pop();
                }
                holder.wordGroup.addView(textView, i);
            }
            int end = start + arabicWords[i].length();
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(fullAyahSpannable.subSequence(start, end));
            start = end + 1;
            TextPaint paint = textView.getPaint();
            Typeface deviceTypeface = paint.getTypeface();
            if (activity.arabicFont != null)
                paint.setTypeface(activity.arabicFont);
            paint.setTextSize(activity.arabicFontSize);
            float width = paint.measureText(stringBuilder + QuranActivity.WORD_SPACE);
            paint.setTypeface(deviceTypeface);
            if (activity.wordCursor != null && activity.wordCursor.getCount() > wordId) {
                activity.wordCursor.moveToPosition(wordId);
                paint.setTextSize(activity.banglaFontSize - 5);
                int wordLangCount = activity.wordCursor.getColumnCount();
                for (int col = 0; col < wordLangCount; col++) {
                    stringBuilder.append("\n");
                    String word = activity.wordCursor.getString(col);
                    int s = stringBuilder.length();
                    stringBuilder.append(word);
                    if (activity.banglaColumn == col)
                        stringBuilder.setSpan(new FontSpan(activity.kalpurush), s, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    float meaningLen = paint.measureText(word + QuranActivity.WORD_SPACE);
                    if (meaningLen > width) width = meaningLen;
                }
            }
            textView.setLayoutParams(new ViewGroup.LayoutParams((int) width,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setText(stringBuilder);
            textView.setTag(++wordId);
        }
        for (int i = viewCount - 1; i >= arabicWords.length; i--) {
            TextView child = (TextView) holder.wordGroup.getChildAt(i);
            holder.wordGroup.removeView(child);
            activity.viewPool.push(child);
        }
    }

    @Override
    public int getItemCount() {
        return activity.mainCursor.getCount();
    }

    @Override
    public void onCompressed(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpg");
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity,
                BuildConfig.APPLICATION_ID + ".provider", file));
        activity.startActivity(intent);
    }


    public class Holder extends RecyclerView.ViewHolder {
        private final TextView headerText, transText, ayahNo;
        private WordGroup wordGroup;

        @SuppressLint("WrongConstant")
        Holder(@NonNull View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.name);
            transText = itemView.findViewById(R.id.trans);
            ayahNo = itemView.findViewById(R.id.ayahNo);
            if (transText != null) {
                transText.setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.banglaFontSize);
                transText.setMovementMethod(LinkMovementMethod.getInstance());
                if (activity.justifyTrans() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    transText.setTextIsSelectable(false);
                    transText.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
                }
            }
            if (ayahNo == null) return;
            ayahNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.banglaFontSize);
            wordGroup = itemView.findViewById(R.id.group);
        }
    }
}
