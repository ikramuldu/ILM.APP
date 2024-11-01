package bn.poro.quran.fragments.subject;

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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
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
import bn.poro.quran.fragments.science.ExpandableItemModel;

class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.Holder>
        implements CompressTask.CompressListener {
    private static final int AYAH_TYPE = 1;
    private static final int NORMAL_TYPE = 0;
    private static final int AYAH_LEVEL = 8;
    private static final int MAX_ID = 4602;
    static final ArrayList<ExpandableItemModel> subjectItems = new ArrayList<>();
    final MainActivity activity;
    private final LinearLayoutManager layoutManager;
    private final Typeface arabicFont;
    private final float arabicFontSize, banglaFontSize;
    private final boolean justifyTrans;
    private final Cursor mainCursor;

    SubjectAdapter(@NonNull MainActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        if (subjectItems.isEmpty()) {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = db.rawQuery("select id,text from subject where type=1",
                    null);
            while (cursor.moveToNext()) {
                subjectItems.add(new ExpandableItemModel(cursor.getInt(0),
                        cursor.getString(1), 1));
            }
            cursor.close();
            db.close();
        }
        float density = activity.getResources().getDisplayMetrics().scaledDensity;
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Activity.MODE_PRIVATE);
        justifyTrans = preferences.getBoolean(Consts.JUSTIFICATION, false);
        banglaFontSize = preferences.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = preferences.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        boolean showArabic = preferences.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        int fontId = preferences.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (showArabic && fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        else arabicFont = null;
        File file = new File(Utils.dataPath + "5120.db");
        if (!file.exists()) try {
            Utils.copy(activity.getAssets().open(file.getName()), file);
        } catch (Exception e) {
            L.d(e);
        }
        SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(),
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        if (showArabic) {
            String query;
            if (fontId >= 5) query = "select indo";
            else query = "select quran.text";
            database.execSQL("ATTACH DATABASE ? AS q", new String[]{Utils.dataPath + Consts.QURAN_DB_NAME});
            mainCursor = database.rawQuery(query + ",content.text from content inner join quran on " +
                    "content.rowid=quran.rowid", null);
        } else
            mainCursor = database.rawQuery("select * from content", null);
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
        if (subjectItems.get(position).level == AYAH_LEVEL) return AYAH_TYPE;
        return NORMAL_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ExpandableItemModel item = subjectItems.get(position);
        if (holder.ayahNo == null) {
            holder.icon.setRotation(item.isExpanded ? 90 : 0);
            holder.itemView.setPadding(((item.level - 1) * holder.textView.getPaddingBottom()),
                    0, 0, 0);
            holder.textView.setText(item.title);
        } else {
            int sura = item.id - 1;
            int ayah = Integer.parseInt(item.title);
            holder.ayahNo.setText(String.format("%s %d:%d",
                    activity.getSuraName(sura), sura + 1, ayah));
            mainCursor.moveToPosition(activity.startIndexOfSura[sura] + ayah);
            if (mainCursor.getColumnCount() == 2) {
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
                stringBuilder.append("\n\n");
                stringBuilder.append(mainCursor.getString(1));
                holder.textView.setText(stringBuilder);
            } else {
                holder.textView.setText(mainCursor.getString(0));
            }
        }
    }

    @Override
    public int getItemCount() {
        return subjectItems.size();
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
            int position = getAdapterPosition();
            if (viewId == R.id.bookmark) {
                ExpandableItemModel item = subjectItems.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, item.id - 1)
                        .putExtra(Consts.EXTRA_AYAH_NUM, Integer.parseInt(item.title)));
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
            ExpandableItemModel item = subjectItems.get(getAdapterPosition());
            int sura = item.id - 1;
            String ayah = item.title;
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
            } else typedArray.recycle();
            itemView.draw(canvas);
            if (id == R.id.save_screen) {
                MediaStore.Images.Media.insertImage(activity.getContentResolver(), bitmap,
                        String.format("%s, %s-%s.jpg",
                                activity.getSuraName(sura), activity.getString(R.string.quran_ayah), ayah), activity.getString(R.string.app_name));
                Toast.makeText(activity, "saved to Pictures", Toast.LENGTH_SHORT).show();
            } else {
                new CompressTask(bitmap, new File(activity.getCacheDir(), "share"),
                        SubjectAdapter.this).start();
            }
            return true;
        }
    }

    private void expand(int position) {
        ExpandableItemModel selectedItem = subjectItems.get(position);
        int nextPosition = position + 1;
        if (selectedItem.isExpanded) {
            int removed = 0;
            while (subjectItems.size() > nextPosition) {
                ExpandableItemModel nextItem = subjectItems.get(nextPosition);
                if (nextItem.level > selectedItem.level) {
                    subjectItems.remove(nextPosition);
                    removed++;
                } else break;
            }
            selectedItem.isExpanded = false;
            notifyItemChanged(position, R.anim.rotate_back);
            notifyItemRangeRemoved(nextPosition, removed);
            return;
        }
        int startId = selectedItem.id + 1;
        int endId;
        if (nextPosition < getItemCount()) {
            endId = subjectItems.get(nextPosition).id - 1;
        } else endId = MAX_ID;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor;
        ArrayList<ExpandableItemModel> items = new ArrayList<>();
        int count;
        Handler handler = new Handler(Looper.getMainLooper());
        if (startId == endId) {
            cursor = database.rawQuery("select text from subject where id=" + startId, null);
            cursor.moveToFirst();
            String s = cursor.getString(0);
            String[] suras = s.split("\n");
            for (String sura : suras) {
                String[] ayahs = sura.split(",");
                int suraId = Integer.parseInt(ayahs[0]);
                for (int i = 1; i < ayahs.length; i++) {
                    items.add(new ExpandableItemModel(suraId, ayahs[i], AYAH_LEVEL));
                }
            }
            count = items.size();
        } else {
            int nextLevel = selectedItem.level + 1;
            cursor = database.rawQuery(
                    "select id,text from subject where id between ? and ? and type=?",
                    new String[]{String.valueOf(startId),
                            String.valueOf(endId), String.valueOf(nextLevel)});
            count = cursor.getCount();
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                ExpandableItemModel item = new ExpandableItemModel(cursor.getInt(0),
                        cursor.getString(1), nextLevel);
                items.add(item);
            }
            if (count == 1) handler.post(() -> expand(nextPosition));
        }
        selectedItem.isExpanded = true;
        notifyItemChanged(position, R.anim.rotate);
        subjectItems.addAll(nextPosition, items);
        notifyItemRangeInserted(nextPosition, count);
        cursor.close();
        database.close();
        int p;
        if (count < Consts.SCROLL_DOWN_LIMIT) p = position + count;
        else p = position + Consts.SCROLL_DOWN_LIMIT;
        handler.post(() -> layoutManager.scrollToPosition(p));
        handler.postDelayed(() -> layoutManager.scrollToPosition(position), 200);
    }
}
