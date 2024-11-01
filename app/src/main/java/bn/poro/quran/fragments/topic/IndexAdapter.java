package bn.poro.quran.fragments.topic;

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
import android.graphics.Color;
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

class IndexAdapter extends RecyclerView.Adapter<IndexAdapter.Holder> implements CompressTask.CompressListener {
    private final ArrayList<Object> items;
    private final MainActivity activity;
    private final Typeface arabicFont;
    private final float arabicFontSize;
    private final float banglaFontSize;
    private final boolean justifyTrans;
    private final Cursor mainCursor;
    private final RecyclerView.LayoutManager layoutManager;

    IndexAdapter(MainActivity activity, RecyclerView.LayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        items = new ArrayList<>();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,bn,en,ayah from concept where parent=0 order by en", null);
        while (cursor.moveToNext()) {
            IndexGroupModel item = new IndexGroupModel(cursor, 0);
            items.add(item);
        }
        cursor.close();
        database.close();
        float density = activity.getResources().getDisplayMetrics().scaledDensity;
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Activity.MODE_PRIVATE);
        justifyTrans = preferences.getBoolean(Consts.JUSTIFICATION, false);
        banglaFontSize = preferences.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = preferences.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        int fontId = preferences.getInt(Consts.ARABIC_FONT_FACE, 1);
        boolean showArabic = preferences.getBoolean(Consts.SHOW_ARABIC_KEY, true);
        if (showArabic && fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        else arabicFont = null;
        File file = new File(Utils.dataPath + "5120.db");
        if (!file.exists()) try {
            Utils.copy(activity.getAssets().open(file.getName()), file);
        } catch (Exception e) {
            L.d(e);
        }
        SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(),
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        if (showArabic) {
            String query;
            if (fontId >= 5) query = "select indo";
            else query = "select quran.text";
            db.execSQL("ATTACH DATABASE ? AS q", new String[]{Utils.dataPath + Consts.QURAN_DB_NAME});
            mainCursor = db.rawQuery(query + ",content.text from content inner join quran on " +
                    "content.rowid=quran.rowid", null);
        } else
            mainCursor = db.rawQuery("select * from content", null);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object obj : payloads) {
            holder.icon.setRotation(0);
            holder.icon.setAnimation(AnimationUtils.loadAnimation(activity, (Integer) obj));
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(
                viewType == 0 ? R.layout.item_subject : R.layout.ayah_subject, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Object object = items.get(position);
        if (object instanceof IndexAyahModel) {
            IndexAyahModel item = (IndexAyahModel) object;
            int sura = item.sura;
            int ayah = item.ayah;
            holder.ayahNo.setText(String.format("%s %d:%d %s",
                    activity.getSuraName(sura), sura + 1, ayah, item.words));
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
                try {
                    int word = Integer.parseInt(item.words);
                    int start = 0;
                    while (word > 1) {
                        start = arabicText.indexOf(" ", start) + 1;
                        word--;
                    }
                    int end = arabicText.indexOf(" ", start);
                    if (end == -1) end = len;
                    stringBuilder.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    L.d(e);
                }
                stringBuilder.append("\n\n");
                stringBuilder.append(mainCursor.getString(1));
                holder.textView.setText(stringBuilder);
            } else {
                holder.textView.setText(mainCursor.getString(0));
            }
        } else {
            IndexGroupModel item = (IndexGroupModel) object;
            if (item.isExpanded) holder.icon.setRotation(90);
            else holder.icon.setRotation(0);
            holder.itemView.setPadding(item.level * 20, 0, 0, 0);
            holder.textView.setText(item.title);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof IndexAyahModel) return 1;
        return 0;
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
                IndexAyahModel item = (IndexAyahModel) items.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, item.sura)
                        .putExtra(Consts.EXTRA_AYAH_NUM, item.ayah));
            } else if (viewId == R.id.icon) {
                PopupMenu popup = new PopupMenu(activity, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.ayah_popup2, menu);
                popup.show();
                popup.setOnMenuItemClickListener(this);
            } else
                expand(position);
        }


        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int id = menuItem.getItemId();
            IndexAyahModel item = (IndexAyahModel) items.get(getAdapterPosition());
            if (id == R.id.play_ayah) {
                View view = LayoutInflater.from(activity).inflate(R.layout.add_mark, null);
                RecyclerView recyclerView = view.findViewById(R.id.main_list);
                recyclerView.setLayoutManager(new MyLayoutManager(activity));
                EditText addCat = view.findViewById(R.id.addCat);
                EditText note = view.findViewById(R.id.note);
                AddBookmarkAdapter adapter = new AddBookmarkAdapter(activity,
                        String.valueOf(item.sura), String.valueOf(item.ayah), addCat, note);
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
                                activity.getSuraName(item.sura), activity.getString(R.string.quran_ayah), item.ayah), activity.getString(R.string.app_name));
                Toast.makeText(activity, "saved to Pictures", Toast.LENGTH_SHORT).show();
            } else {
                new CompressTask(bitmap, new File(activity.getCacheDir(), "share"),
                        IndexAdapter.this).start();
            }
            return true;
        }
    }

    private void expand(int position) {
        IndexGroupModel selectedItem = (IndexGroupModel) items.get(position);
        int nextPosition = position + 1;
        if (selectedItem.isExpanded) {
            int removed = 0;
            while (items.size() > nextPosition) {
                Object nextItem = items.get(nextPosition);
                if (nextItem instanceof IndexAyahModel || ((IndexGroupModel) nextItem).level > selectedItem.level) {
                    items.remove(nextPosition);
                    removed++;
                } else break;
            }
            selectedItem.isExpanded = false;
            notifyItemChanged(position, R.anim.rotate_back);
            notifyItemRangeRemoved(nextPosition, removed);
            return;
        }
        selectedItem.isExpanded = true;
        notifyItemChanged(position, R.anim.rotate);
        int nextLevel = selectedItem.level + 1;
        SQLiteDatabase database;
        Cursor cursor;
        ArrayList<Object> children;
        if (selectedItem.ayahs != null) {
            String[] strings = selectedItem.ayahs.split("\n");
            String[] wordList = strings[1].split(",");
            database = SQLiteDatabase.openDatabase(Utils.dataPath +
                    Consts.QURAN_DB_NAME, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            cursor = database.rawQuery("select sura,ayah from quran where rowid in (" + strings[0] + ")", null);
            children = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                children.add(new IndexAyahModel(cursor.getInt(0), cursor.getInt(1), wordList[cursor.getPosition()]));
            }

        } else {
            database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            cursor = database.rawQuery("select id,bn,en,ayah from concept where parent=? order by en desc", new String[]{String.valueOf(selectedItem.id)});
            children = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                children.add(new IndexGroupModel(cursor, nextLevel));
            }
        }
        items.addAll(nextPosition, children);
        notifyItemRangeInserted(nextPosition, children.size());
        cursor.close();
        database.close();
        int p;
        if (children.size() < Consts.SCROLL_DOWN_LIMIT) p = position + children.size();
        else p = position + Consts.SCROLL_DOWN_LIMIT;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> layoutManager.scrollToPosition(p));
        handler.postDelayed(() -> layoutManager.scrollToPosition(position), 200);
    }
}
