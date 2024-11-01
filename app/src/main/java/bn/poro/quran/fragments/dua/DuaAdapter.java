package bn.poro.quran.fragments.dua;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.activity_quran.AddBookmarkAdapter;
import bn.poro.quran.BuildConfig;
import bn.poro.quran.CompressTask;
import bn.poro.quran.Consts;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.R;
import bn.poro.quran.Utils;


class DuaAdapter extends RecyclerView.Adapter<DuaAdapter.Holder>
        implements CompressTask.CompressListener {
    private static final int AYAH_TYPE = 1;
    private static final int NORMAL_TYPE = 0;
    static final ArrayList<DuaItem> duaItems = new ArrayList<>();
    final MainActivity activity;
    private final Typeface arabicFont;
    private final float arabicFontSize, banglaFontSize;
    private final boolean justifyTrans;
    private final Cursor mainCursor;
    private final LinearLayoutManager layoutManager;

    DuaAdapter(@NonNull MainActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        if (duaItems.isEmpty()) {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor subCursor = db.rawQuery("select name,topic,source from dua",
                    null);
            while (subCursor.moveToNext()) {
                DuaItem item = new DuaItem(subCursor.getString(0),
                        subCursor.getString(1), subCursor.getString(2));
                duaItems.add(item);
            }
            subCursor.close();
            db.close();
        }
        float density = activity.getResources().getDisplayMetrics().scaledDensity;
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Activity.MODE_PRIVATE);
        justifyTrans = preferences.getBoolean(Consts.JUSTIFICATION, false);
        banglaFontSize = preferences.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = preferences.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        boolean showTrans = preferences.getBoolean(Consts.SHOW_TRANS, true);
        int fontId = preferences.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        else arabicFont = null;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        String query;
        if (fontId >= 5) query = "select indo";
        else query = "select quran.text";
        if (showTrans) {
            File file = new File(Utils.dataPath + "5120.db");
            if (!file.exists()) try {
                Utils.copy(activity.getAssets().open(file.getName()), file);
            } catch (Exception e) {
                L.d(e);
            }
            database.execSQL("ATTACH DATABASE ? AS trans", new String[]{file.getPath()});
            mainCursor = database.rawQuery(query + ",content.text from quran inner join content on " +
                    "content.rowid=quran.rowid", null);
        } else
            mainCursor = database.rawQuery(query + " from quran", null);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            holder.icon.setAnimation(AnimationUtils.loadAnimation(activity, (int) payload));

        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(viewType == AYAH_TYPE ?
                R.layout.ayah_subject : R.layout.item_subject, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (duaItems.get(position).source == null) return AYAH_TYPE;
        return NORMAL_TYPE;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DuaItem item = duaItems.get(position);
        if (holder.ayahNo == null) {
            holder.icon.setRotation(item.isExpanded ? 90 : 0);
            SpannableString string = new SpannableString(item.topic + "\n" + item.name);
            int start = item.topic.length() + 1;
            int end = string.length();
            string.setSpan(new RelativeSizeSpan(0.9f), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            string.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.textView.setText(string);
        } else {
            int sura = Integer.parseInt(item.name) - 1;
            int ayah = Integer.parseInt(item.topic);
            holder.ayahNo.setText(String.format( "%s %d:%d",
                    activity.getSuraName(sura), sura + 1, ayah));
            mainCursor.moveToPosition(activity.startIndexOfSura[sura] + ayah);
            String arabicText = mainCursor.getString(0);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(arabicText);
            int len = arabicText.length();
            if (arabicFont != null)
                stringBuilder.setSpan(new FontSpan(arabicFont), 0,
                        len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new AbsoluteSizeSpan((int) arabicFontSize, false),
                    0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (arabicText.endsWith("۩")) {
                stringBuilder.setSpan(new ForegroundColorSpan(0xff81d4fa),
                        len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (mainCursor.getColumnCount() == 2) {
                stringBuilder.append("\n\n");
                stringBuilder.append(mainCursor.getString(1));
            }
            holder.textView.setText(stringBuilder);
        }
    }

    @Override
    public int getItemCount() {
        return duaItems.size();
    }

    @Override
    public void onCompressed(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpg");
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity,
                BuildConfig.APPLICATION_ID + ".provider", file));
        activity.startActivity(intent);
    }


    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
        final TextView textView, ayahNo;
        final ImageView icon;

        @SuppressLint("WrongConstant")
        public Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            textView = itemView.findViewById(R.id.text);
            if (itemView.getId() == R.id.subject) {
                itemView.setOnClickListener(this);
                ayahNo = null;
            } else {
                ayahNo = itemView.findViewById(R.id.ayahNo);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, banglaFontSize);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                ayahNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, mainCursor.getColumnCount() == 2 &&
                        arabicFontSize - 6 > banglaFontSize ?
                        arabicFontSize - 3 : banglaFontSize + 3);
                if (justifyTrans && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    textView.setTextIsSelectable(false);
                    textView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
                }
                itemView.findViewById(R.id.bookmark).setOnClickListener(this);
                icon.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            int position = getLayoutPosition();
            if (viewId == R.id.bookmark) {
                DuaItem item = duaItems.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, Integer.parseInt(item.name) - 1)
                        .putExtra(Consts.EXTRA_AYAH_NUM, Integer.parseInt(item.topic)));
            } else if (viewId == R.id.icon) {
                PopupMenu popup = new PopupMenu(activity, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.ayah_popup2, menu);
                popup.show();
                popup.setOnMenuItemClickListener(this);
            } else expand(position);
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int id = menuItem.getItemId();
            DuaItem item = duaItems.get(getLayoutPosition());
            int sura = Integer.parseInt(item.name) - 1;
            String ayah = item.topic;
            if (id == R.id.play_ayah) {
                View view = LayoutInflater.from(activity).inflate(R.layout.add_mark, null);
                RecyclerView recyclerView = view.findViewById(R.id.main_list);
                recyclerView.setLayoutManager(new MyLayoutManager(activity));
                EditText addCat = view.findViewById(R.id.addCat);
                EditText note = view.findViewById(R.id.note);
                AddBookmarkAdapter adapter = new AddBookmarkAdapter(activity,
                        String.valueOf(sura), ayah, addCat, note);
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
                StringBuilder stringBuilder = new StringBuilder(textView.getText());
                stringBuilder.append("\n\n");
                stringBuilder.append(ayahNo.getText());
                if (id == R.id.copy_text) {
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("label", stringBuilder));
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast.makeText(activity, R.string.ayah_copied_popup, Toast.LENGTH_SHORT).show();
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
            } else typedArray.recycle();
            itemView.draw(canvas);
            if (id == R.id.save_screen) {
                MediaStore.Images.Media.insertImage(activity.getContentResolver(), bitmap,
                        String.format( "%s, %s-%s.jpg",
                                activity.getSuraName(sura), activity.getString(R.string.quran_ayah), ayah), activity.getString(R.string.app_name));
                Toast.makeText(activity, "saved to Pictures", Toast.LENGTH_SHORT).show();
            } else {
                new CompressTask(bitmap, new File(activity.getCacheDir(), "share"),
                        DuaAdapter.this).start();
            }
            return true;
        }
    }

    private void expand(int position) {
        DuaItem selectedItem = duaItems.get(position);
        int nextPosition = position + 1;
        if (selectedItem.isExpanded) {
            int removed = 0;
            while (nextPosition < duaItems.size()) {
                DuaItem nextItem = duaItems.get(nextPosition);
                if (nextItem.source == null) {
                    duaItems.remove(nextPosition);
                    removed++;
                } else break;
            }
            selectedItem.isExpanded = false;
            notifyItemChanged(position, R.anim.rotate_back);
            notifyItemRangeRemoved(nextPosition, removed);
            return;
        }
        int count = 1;
        String[] strings = selectedItem.source.split(":");
        if (strings[1].contains("-")) {
            String[] ayahRange = strings[1].split("-");
            int start = Integer.parseInt(ayahRange[0]);
            int end = Integer.parseInt(ayahRange[1]);
            count += end - start;
            while (start <= end) {
                duaItems.add(nextPosition, new DuaItem(strings[0], String.valueOf(end), null));
                end--;
            }
        } else {
            duaItems.add(nextPosition, new DuaItem(strings[0], strings[1], null));
        }
        selectedItem.isExpanded = true;
        notifyItemChanged(position, R.anim.rotate);
        notifyItemRangeInserted(nextPosition, count);
        new Handler(Looper.getMainLooper()).post(() -> layoutManager.scrollToPosition(nextPosition));
    }
}
