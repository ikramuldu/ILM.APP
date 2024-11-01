package bn.poro.quran.fragments.name;

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

class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.Holder>
        implements CompressTask.CompressListener {
    private static final int AYAH_TYPE = 1;
    private static final int PLAIN_TEXT_TYPE = 2;
    private static final int EXPANDABLE_TYPE = 0;
    static final ArrayList<NameItem> nameItems = new ArrayList<>();
    final MainActivity activity;
    private final Typeface arabicFont;
    private final float arabicFontSize, banglaFontSize;
    private final boolean justifyTrans;
    private final Cursor mainCursor;
    private final LinearLayoutManager layoutManager;

    NamesAdapter(@NonNull MainActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        if (nameItems.isEmpty()) {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor subCursor = db.rawQuery("select text,source from names",
                    null);
            while (subCursor.moveToNext()) {
                NameItem item = new NameItem(subCursor.getString(0),
                        subCursor.getString(1), true);
                nameItems.add(item);
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
                R.layout.ayah_subject : viewType == PLAIN_TEXT_TYPE ?
                R.layout.textview : R.layout.item_subject, parent, false));
    }


    @Override
    public int getItemViewType(int position) {
        NameItem item = nameItems.get(position);
        if (item.expandable) return EXPANDABLE_TYPE;
        if (item.source == null) return PLAIN_TEXT_TYPE;
        return AYAH_TYPE;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        NameItem item = nameItems.get(position);
        if (holder.icon == null) {
            holder.textView.setText(item.name);
        } else if (holder.ayahNo == null) {
            holder.icon.setRotation(item.isExpanded ? 90 : 0);
            holder.textView.setText(item.name);
        } else {
            int sura = Integer.parseInt(item.name) - 1;
            int ayah = Integer.parseInt(item.source);
            holder.ayahNo.setText(String.format("%s %d:%d",
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
        return nameItems.size();
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
        final TextView textView;
        TextView ayahNo;
        ImageView icon;

        @SuppressLint("WrongConstant")
        public Holder(@NonNull View itemView) {
            super(itemView);
            int id = itemView.getId();
            if (id == R.id.text) {
                textView = (TextView) itemView;
                return;
            }
            icon = itemView.findViewById(R.id.icon);
            textView = itemView.findViewById(R.id.text);
            if (id == R.id.subject) {
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
                NameItem item = nameItems.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, Integer.parseInt(item.name) - 1)
                        .putExtra(Consts.EXTRA_AYAH_NUM, Integer.parseInt(item.source)));
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
            NameItem item = nameItems.get(getLayoutPosition());
            int sura = Integer.parseInt(item.name) - 1;
            String ayah = item.source;
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
                        NamesAdapter.this).start();
            }
            return true;
        }
    }

    private void expand(int position) {
        NameItem selectedItem = nameItems.get(position);
        int nextPosition = position + 1;
        if (selectedItem.isExpanded) {
            int removed = 0;
            while (nextPosition < nameItems.size()) {
                NameItem nextItem = nameItems.get(nextPosition);
                if (!nextItem.expandable) {
                    nameItems.remove(nextPosition);
                    removed++;
                } else break;
            }
            selectedItem.isExpanded = false;
            notifyItemChanged(position, R.anim.rotate_back);
            notifyItemRangeRemoved(nextPosition, removed);
            return;
        }
        int count;
        if (selectedItem.source.contains(":")) {
            String[] strings = selectedItem.source.split(",");
            count = strings.length;
            for (String s : strings) {
                String[] parts = s.split(":");
                nameItems.add(nextPosition, new NameItem(parts[0], parts[1], false));
            }
        } else {
            nameItems.add(nextPosition, new NameItem(selectedItem.source, null, false));
            count = 1;
        }
        selectedItem.isExpanded = true;
        notifyItemChanged(position, R.anim.rotate);
        notifyItemRangeInserted(nextPosition, count);
        new Handler(Looper.getMainLooper()).post(() -> layoutManager.scrollToPosition(nextPosition));
    }
}
