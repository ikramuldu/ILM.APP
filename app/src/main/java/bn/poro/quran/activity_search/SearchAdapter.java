package bn.poro.quran.activity_search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_quran.AddBookmarkAdapter;
import bn.poro.quran.CompressTask;
import bn.poro.quran.Consts;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.activity_setting.SettingActivity;

class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.Holder> implements CompressTask.CompressListener {
    private final SearchQuranActivity activity;
    private ArrayList<SearchTask.SearchResult> searchResult;
    private SearchTask.ResultItem[] resultItems;
    private final int tabTextColor;
    private final Typeface arabicFont, kalpurush, quranBangla;
    private final float arabicFontSize;
    private final float banglaFontSize;
    private final boolean justifyTrans, showArabic;

    SearchAdapter(SearchQuranActivity activity) {
        this.activity = activity;
        float density = activity.getResources().getDisplayMetrics().scaledDensity;
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Activity.MODE_PRIVATE);
        justifyTrans = preferences.getBoolean(Consts.JUSTIFICATION, false);
        banglaFontSize = preferences.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = preferences.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        showArabic = preferences.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        int fontId = preferences.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        else arabicFont = null;
        kalpurush = ResourcesCompat.getFont(activity, R.font.kalpurush);
        quranBangla = ResourcesCompat.getFont(activity, R.font.banglaquran);
        TypedArray typedArray = activity.obtainStyledAttributes(new int[]{R.attr.tab_text, R.attr.inactive});
        tabTextColor = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else
            typedArray.recycle();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(R.layout.ayah_search, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SearchTask.ResultItem item = resultItems[position];
        holder.transText.setTag(item.id);
        holder.ayahNo.setText(String.format( "%s %d:%d",
                activity.suraNames[item.sura], item.sura + 1, item.ayah));
        StringBuilder regex = new StringBuilder();
        char[] chars = activity.string.toCharArray();
        for (char c : chars) {
            regex.append(c).append("[\u064B-\u0670]*");
        }
        String arabicText = item.text.replaceAll(" ", "  ");
        //matcher for complete word toward end
        Matcher matcher = Pattern.compile(regex.append(".*?\\b").toString(), Pattern.CASE_INSENSITIVE).matcher(arabicText);
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (matcher.find() || (showArabic && item.ayah > 0)) {
            stringBuilder.append(arabicText);
            int len = arabicText.length();
            if (arabicFont != null)
                stringBuilder.setSpan(new FontSpan(arabicFont), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new AbsoluteSizeSpan((int) arabicFontSize, false), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (arabicText.endsWith("۩")) {
                stringBuilder.setSpan(new ForegroundColorSpan(0xff81d4fa), len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        for (int i = searchResult.size() - 1; i >= 0; i--) {
            SearchTask.SearchResult result = searchResult.get(i);
            String string = result.trans.get(item.id);
            if (string == null) continue;
            if (stringBuilder.length() > 0)
                stringBuilder.append("\n\n");
            int start = stringBuilder.length();
            stringBuilder.append(result.title);
            int end = stringBuilder.length();
            stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new ForegroundColorSpan(tabTextColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.append("\n");
            String br;
            if (Utils.notContains(Consts.RTL, result.title.substring(0, result.title.indexOf('-')))) {
                stringBuilder.append("\u200E");
                br = "<br>\u200E";
            } else br = "<br>";
            stringBuilder.append(Html.fromHtml(string.replaceAll("\n", br)));
            end = stringBuilder.length();
            if (result.title.startsWith("Bengali")) {
                if (result.id == Consts.BENGALI_TAJWEED_TRANSLITERATION_ID)
                    stringBuilder.setSpan(new FontSpan(quranBangla), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else
                    stringBuilder.setSpan(new FontSpan(kalpurush), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        matcher.reset(stringBuilder);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            stringBuilder.setSpan(new ForegroundColorSpan(0xffdd0000), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.transText.setText(stringBuilder);
    }

    @Override
    public int getItemCount() {
        return resultItems == null ? 0 : resultItems.length;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(ArrayList<SearchTask.SearchResult> searchResult, SearchTask.ResultItem[] mainCursor) {
        this.searchResult = searchResult;
        this.resultItems = mainCursor;
        notifyDataSetChanged();
    }

    @Override
    public void onCompressed(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpg");
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", file));
        activity.startActivity(intent);
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        public final TextView transText, ayahNo;

        @SuppressLint("WrongConstant")
        public Holder(@NonNull View itemView) {
            super(itemView);
            transText = itemView.findViewById(R.id.text);
            ayahNo = itemView.findViewById(R.id.ayahNo);
            transText.setTextSize(TypedValue.COMPLEX_UNIT_PX, banglaFontSize);
            transText.setMovementMethod(LinkMovementMethod.getInstance());
            ayahNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, showArabic && arabicFontSize - 6 > banglaFontSize ?
                    arabicFontSize - 3 : banglaFontSize + 3);
            if (justifyTrans && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                transText.setTextIsSelectable(false);
                transText.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
            itemView.findViewById(R.id.bookmark).setOnClickListener(this);
            itemView.findViewById(R.id.icon).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            SearchTask.ResultItem item = resultItems[getLayoutPosition()];
            if (v.getId() == R.id.bookmark) {
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, item.sura)
                        .putExtra(Consts.EXTRA_AYAH_NUM, item.ayah));
            } else {
                PopupMenu popup = new PopupMenu(activity, v);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.ayah_popup2, menu);
                popup.show();
                popup.setOnMenuItemClickListener(this);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            SearchTask.ResultItem item = resultItems[getLayoutPosition()];
            int id = menuItem.getItemId();
            if (id == R.id.play_ayah) {
                View view = LayoutInflater.from(activity).inflate(R.layout.add_mark, null);
                RecyclerView recyclerView = view.findViewById(R.id.main_list);
                recyclerView.setLayoutManager(new MyLayoutManager(activity));
                EditText addCat = view.findViewById(R.id.addCat);
                EditText note = view.findViewById(R.id.note);
                AddBookmarkAdapter adapter = new AddBookmarkAdapter(activity, String.valueOf(item.sura), String.valueOf(item.ayah), addCat, note);
                recyclerView.setAdapter(adapter);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.select_folder)
                        .setView(view)
                        .setPositiveButton(R.string.mtrl_picker_save, (dialog, which) ->
                                new Handler(Looper.getMainLooper()).post(adapter::save))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            }
            if (id == R.id.copy_text || id == R.id.share_text) {
                StringBuilder stringBuilder = new StringBuilder(transText.getText());
                stringBuilder.append("\n\n");
                stringBuilder.append(ayahNo.getText());
                if (id == R.id.copy_text) {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("label", stringBuilder));
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)  Toast.makeText(activity, R.string.ayah_copied_popup, Toast.LENGTH_SHORT).show();
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
                MediaStore.Images.Media.insertImage(activity.getContentResolver(), bitmap, String.format( "%s, %s-%d.jpg", activity.suraNames[item.sura], activity.getString(R.string.quran_ayah), item.ayah), activity.getString(R.string.app_name));
                Toast.makeText(activity, "saved to Pictures", Toast.LENGTH_SHORT).show();
            } else {
                new CompressTask(bitmap, new File(activity.getCacheDir(), "share"), SearchAdapter.this).start();
            }
            return true;
        }
    }
}
