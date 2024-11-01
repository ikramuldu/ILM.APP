package bn.poro.quran.activity_quran;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.CompressTask;
import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.my_paging.PagerDataModel;
import bn.poro.quran.my_paging.PagerDataProvider;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.views.WordGroup;

public class FullQuranAdapter extends RecyclerView.Adapter<FullQuranAdapter.Holder> implements CompressTask.CompressListener, PagerDataProvider<AyahItem> {
    private static final int TYPE_SURA = 0;
    private static final int TYPE_JUZ = 2;
    private static final int TYPE_AYAH = 1;
    private static final int HIGHLIGHT_WHOLE_AYAH = 0;
    final QuranActivity activity;
    final LinearLayoutManager layoutManager;
    private final PagerDataModel<AyahItem> dataModel;
    private static final int TYPE_PLACEHOLDER = 3;
    private final ArrayList<Integer> bookmarks;

    FullQuranAdapter(QuranActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        bookmarks = new ArrayList<>();
        dataModel = new PagerDataModel<>(this, layoutManager, this);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        @LayoutRes int layout;
        LayoutInflater inflater = LayoutInflater.from(activity);
        switch (viewType) {
            case TYPE_PLACEHOLDER:
                return new Holder(inflater.inflate(R.layout.place_holder, parent, false));
            case TYPE_SURA:
                layout = R.layout.only_head;
                break;
            case TYPE_JUZ:
                layout = R.layout.with_head;
                break;
            default:
                layout = R.layout.item_ayah;
        }
        ViewGroup view = (ViewGroup) inflater.inflate(layout, parent, false);
        if (activity.hasTranslation()) inflater.inflate(R.layout.ayah_trans, view);
        return new Holder(view);
    }

    @Override
    public int getItemViewType(int position) {
        AyahItem item = dataModel.getItem(position);
        if (item == null) return TYPE_PLACEHOLDER;
        if (item.ayah == 0) return TYPE_SURA;
        if (item.juz != 0) return TYPE_JUZ;
        return TYPE_AYAH;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty())
            onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            Object[] objects = (Object[]) payload;
            int wordPosition = (int) objects[0];
            boolean wordPressed = (boolean) objects[1];
            boolean showArabic = holder.wordGroup != null;
            if (wordPosition == HIGHLIGHT_WHOLE_AYAH) {
                if (wordPressed) {
                    holder.itemView.setBackgroundColor(activity.playingAyahBackColor);
                    //if (showArabic && PlayerService.wordByWord) holder.wordGroup.getChildAt(0).setPressed(true);
                } else {
                    holder.itemView.setBackgroundColor(0);
                    if (showArabic && activity.playerService != null && activity.playerService.playingWordByWord()) {
                        View view = holder.wordGroup.getChildAt(holder.wordGroup.getChildCount() - 1);
                        if (view != null)
                            view.setPressed(false);
                    }
                }
            } else if (showArabic && wordPosition <= holder.wordGroup.getChildCount()) {
                View wordView = holder.wordGroup.getChildAt(wordPosition - 1);
                if (wordView == null) continue;
                wordView.setPressed(wordPressed);
                int topOfPressedWord = wordView.getTop();
                if (!wordPressed && activity.scrollWithPlayer() && topOfPressedWord != activity.wordScrolledOffset) {
                    activity.wordScrolledOffset = topOfPressedWord;
                    new Handler(Looper.getMainLooper()).post((() -> {
                        if (layoutManager.findLastVisibleItemPosition() <= activity.playingPosition) {
                            layoutManager.scrollToPositionWithOffset(activity.playingPosition, -activity.wordScrolledOffset);
                        }
                    }));
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AyahItem item = dataModel.getItem(position);
        if (item == null) return;
        holder.itemView.setTag(item);
        if (item.translations != null) {
            SpannableStringBuilder translationBuilder = new SpannableStringBuilder();
            for (int i = 0; i < item.translations.length; i++) {
                String string = item.translations[i];
                if (string == null) continue;
                String name = activity.translationNames.valueAt(i);
                if (translationBuilder.length() > 0)
                    translationBuilder.append("\n\n");
                int start, end;
                if (item.translations.length > 1) {
                    start = translationBuilder.length();
                    translationBuilder.append(name);
                    end = translationBuilder.length();
                    translationBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    translationBuilder.setSpan(new ForegroundColorSpan(activity.tabTextColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    translationBuilder.append("\n");
                }
                start = translationBuilder.length();
                translationBuilder.append(Utils.processTranslation(activity, name, string));
                end = translationBuilder.length();
                if (name.startsWith("bn")) {
                    if (activity.translationNames.keyAt(i) == Consts.BENGALI_TAJWEED_TRANSLITERATION_ID)
                        translationBuilder.setSpan(new FontSpan(activity.quranBangla), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    else
                        translationBuilder.setSpan(new FontSpan(activity.kalpurush), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            if (translationBuilder.length() == 0) {
                holder.transText.setVisibility(View.GONE);
            } else {
                holder.transText.setVisibility(View.VISIBLE);
                holder.transText.setText(translationBuilder);
            }
        }
        if (holder.headerText != null) {
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            if (item.juz != 0) {
                stringBuilder.append(activity.getString(R.string.quran_juz2));
                stringBuilder.append(": ");
                stringBuilder.append(String.valueOf(Utils.formatNum(item.juz)));
                stringBuilder.setSpan(new RelativeSizeSpan(0.85f), 0,
                        stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (item.ayah == 0) stringBuilder.append("\n");
            }
            int sura = item.suraIndex;
            if (item.ayah == 0) {
                stringBuilder.append(Utils.formatNum(sura + 1));
                stringBuilder.append(". ");
                stringBuilder.append(QuranActivity.allSuraInfo.get(sura).name);
                stringBuilder.append("\n");
                int start = stringBuilder.length();
                stringBuilder.append(activity.getString(R.string.sura_num_ayahs));
                stringBuilder.append(' ');
                stringBuilder.append(Utils.formatNum(QuranActivity.allSuraInfo.get(sura).totalAyah));
                if (QuranActivity.allSuraInfo.get(sura).makki) {
                    stringBuilder.append(", ");
                    stringBuilder.append(activity.getString(R.string.makki));
                } else {
                    stringBuilder.append(", ");
                    stringBuilder.append(activity.getString(R.string.madani));
                }
                stringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.append("\n");
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
        if (position == activity.playingPosition)
            holder.itemView.setBackgroundColor(activity.playingAyahBackColor);
        else holder.itemView.setBackgroundColor(0);
        holder.ayahNo.setText(Utils.formatNum(item.ayah));
        if (bookmarks.contains(item.suraIndex * 1000 + item.ayah))
            holder.bookmark.setImageResource(R.drawable.ic_bookmark_on);
        else holder.bookmark.setImageResource(R.drawable.ic_bookmark);
        if (activity.withoutArabic()) return;
        int viewCount = holder.wordGroup.getChildCount();
        LayoutInflater inflater = activity.getLayoutInflater();
        String arabicText = item.arabicText;
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
        int wordId = item.wordStart - 1;
        for (int i = 0; i < arabicWords.length; i++) {
            TextView textView;
            if (i < viewCount) textView = (TextView) holder.wordGroup.getChildAt(i);
            else {
                if (activity.viewPool.isEmpty()) {
                    textView = (TextView) inflater.inflate(R.layout.word, holder.wordGroup, false);
                    textView.setOnClickListener(activity.clickHandler);
                    textView.setOnLongClickListener(activity.clickHandler);
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
            if (item.words != null) {
                paint.setTextSize(activity.banglaFontSize);
                String[] words = item.words[i];
                for (int col = 0; col < words.length; col++) {
                    stringBuilder.append("\n");
                    String word = words[col];
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
            textView.setPressed(wordId == activity.highlightWord);
        }
        for (int i = viewCount - 1; i >= arabicWords.length; i--) {
            TextView child = (TextView) holder.wordGroup.getChildAt(i);
            holder.wordGroup.removeView(child);
            activity.viewPool.push(child);
        }
    }

    @Override
    public int getItemCount() {
        return Consts.ITEM_COUNT;
    }

    @Override
    public void onCompressed(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpg");
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity,
                BuildConfig.APPLICATION_ID + ".provider", file));
        activity.startActivity(intent);
    }

    @Override
    public List<AyahItem> fetch(int request) {
        ArrayList<AyahItem> ayahList = new ArrayList<>();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        String start = String.valueOf(request * PagerDataModel.PAGE_SIZE + 1);
        String end = String.valueOf((request + 1) * PagerDataModel.PAGE_SIZE);
        String[] limit = new String[]{start, end};
        Cursor cursor = database.rawQuery("SELECT sura,ayah," + (activity.fontId >= 5 ? "indo" : "text") + ",juz,word " + "from quran where rowid between ? and ?", limit);
        while (cursor.moveToNext()) {
            AyahItem item = new AyahItem(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4));
            ayahList.add(item);
        }
        cursor.close();
        database.close();
        if (activity.hasTranslation()) {
            int totalTrans = activity.translationNames.size();
            for (int transIndex = 0; transIndex < totalTrans; transIndex++) {
                database = SQLiteDatabase.openDatabase(Utils.dataPath + activity.translationNames.keyAt(transIndex) + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                cursor = database.rawQuery("select text from content where rowid between ? and ?", limit);
                while (cursor.moveToNext()) {
                    AyahItem ayahItem = ayahList.get(cursor.getPosition());
                    if (ayahItem.translations == null)
                        ayahItem.translations = new String[totalTrans];
                    ayahItem.translations[transIndex] = cursor.getString(0);
                }
                cursor.close();
                database.close();
            }
        }
        if (activity.hasWordMeaning()) {
            int ayahCount = ayahList.size();
            AyahItem firstItem = ayahList.get(0);
            if (firstItem.ayah == 0) {
                if (ayahCount < 2) return ayahList;
                firstItem = ayahList.get(1);
            }
            start = String.valueOf(firstItem.wordStart);
            AyahItem lastAyah = ayahList.get(ayahCount - 1);
            if (lastAyah.ayah == 0) {
                if (ayahCount < 2) return ayahList;
                lastAyah = ayahList.get(ayahCount - 2);
            }
            int lastAyahSize = lastAyah.arabicText.split(" ").length;
            end = String.valueOf(lastAyah.wordStart + lastAyahSize);
            int totalWord = activity.wordDBIds.size();
            for (int wordDBIndex = 0; wordDBIndex < totalWord; wordDBIndex++) {
                database = SQLiteDatabase.openDatabase(Utils.dataPath + activity.wordDBIds.get(wordDBIndex) + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                cursor = database.rawQuery("select text from content where rowid between ? and ?", new String[]{start, end});
                for (int ayah = 0; ayah < ayahCount; ayah++) {
                    AyahItem ayahItem = ayahList.get(ayah);
                    if (ayahItem.ayah == 0) continue;
                    int wordCount;
                    try {
                        AyahItem nextItem = ayahList.get(ayah + 1);
                        if (nextItem.ayah == 0)
                            nextItem = ayahList.get(ayah + 2);
                        wordCount = nextItem.wordStart - ayahItem.wordStart;
                    } catch (IndexOutOfBoundsException e) {
                        wordCount = lastAyahSize;
                    }
                    if (ayahItem.words == null) ayahItem.words = new String[wordCount][totalWord];
                    for (int word = 0; word < wordCount; word++) {
                        if (!cursor.moveToNext()) break;
                        ayahItem.words[word][wordDBIndex] = cursor.getString(0);
                    }
                }
                cursor.close();
                database.close();
            }
        }
        return ayahList;
    }

    @Override
    public void init() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select sura,ayah from bookmark", null);
        while (cursor.moveToNext()) {
            Integer a = cursor.getInt(0) * 1000 + cursor.getInt(1);
            L.d("saved: " + a);
            bookmarks.add(a);
        }
        cursor.close();
        database.close();
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        private final TextView headerText, transText, ayahNo;
        private WordGroup wordGroup;
        private ImageView bookmark;

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
            ayahNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, activity.viewPool != null && activity.arabicFontSize - 6 > activity.banglaFontSize ?
                    activity.arabicFontSize - 3 : activity.banglaFontSize + 3);
            bookmark = itemView.findViewById(R.id.bookmark);
            itemView.findViewById(R.id.icon).setOnClickListener(this);
            bookmark.setOnClickListener(this);
            wordGroup = itemView.findViewById(R.id.group);
            wordGroup.setJustification(activity.justifyTrans() && activity.wordDBIds == null);
        }

        @Override
        public void onClick(View v) {
            AyahItem item = dataModel.getItem(getLayoutPosition());
            if (item == null) return;
            if (v.getId() == R.id.bookmark) {
                View view = LayoutInflater.from(activity).inflate(R.layout.add_mark, null);
                RecyclerView recyclerView = view.findViewById(R.id.main_list);
                recyclerView.setLayoutManager(new MyLayoutManager(activity));
                EditText addCat = view.findViewById(R.id.addCat);
                EditText note = view.findViewById(R.id.note);
                AddBookmarkAdapter adapter = new AddBookmarkAdapter(activity, String.valueOf(item.suraIndex), String.valueOf(item.ayah), addCat, note);
                recyclerView.setAdapter(adapter);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.add_bookmarks)
                        .setView(view)
                        .setPositiveButton(R.string.mtrl_picker_save, (dialog, which) -> new Handler(Looper.getMainLooper()).post(() -> {
                            Integer a = Integer.parseInt(adapter.sura) * 1000 + Integer.parseInt(adapter.ayah);
                            if (adapter.save()) {
                                bookmark.setImageResource(R.drawable.ic_bookmark_on);
                                bookmarks.add(a);
                            } else {
                                bookmark.setImageResource(R.drawable.ic_bookmark);
                                bookmarks.remove(a);
                            }
                        }))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                PopupMenu popup = new PopupMenu(activity, v);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.ayah_popup, menu);
                popup.show();
                popup.setOnMenuItemClickListener(this);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            AyahItem item = dataModel.getItem(getLayoutPosition());
            if (item == null) return true;
            int id = menuItem.getItemId();
            if (id == R.id.play_word || id == R.id.play_ayah) {
                ActivityCompat.startForegroundService(activity, new Intent(activity, QuranPlayerService.class)
                        .putExtra(Consts.EXTRA_SURA_ID, item.suraIndex + 1)
                        .putExtra(Consts.EXTRA_AYAH_NUM, item.ayah)
                        .putExtra(Consts.SHOW_BY_WORD, id == R.id.play_word));
                activity.bindService();
                activity.hideScrollButton();
                return true;
            }
            if (id == R.id.copy_text || id == R.id.share_text) {
                StringBuilder stringBuilder = new StringBuilder(item.arabicText);
                if (transText != null) {
                    stringBuilder.append("\n\n");
                    stringBuilder.append(transText.getText());
                }
                if (id == R.id.copy_text) {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("label", stringBuilder));
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(activity, R.string.ayah_copied_popup, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString());
                    activity.startActivity(intent);
                }
                return true;
            }

            Bitmap bitmap = Bitmap.createBitmap(itemView.getWidth(), itemView.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            TypedArray typedArray = activity.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
            canvas.drawColor(typedArray.getColor(0, 0));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                typedArray.close();
            } else
                typedArray.recycle();
            itemView.draw(canvas);
            if (id == R.id.save_screen) {
                MediaStore.Images.Media.insertImage(activity.getContentResolver(), bitmap, String.format("%s, %s-%d.jpg", QuranActivity.allSuraInfo.get(item.suraIndex).name, activity.getString(R.string.quran_ayah), item.ayah), activity.getString(R.string.app_name));
                Toast.makeText(activity, "saved to Pictures", Toast.LENGTH_SHORT).show();
            } else {
                new CompressTask(bitmap, new File(activity.getCacheDir(), "share"), FullQuranAdapter.this).start();
            }
            return true;
        }

    }
}
