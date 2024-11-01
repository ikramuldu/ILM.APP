package bn.poro.quran.fragments.sajdah;

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

import bn.poro.quran.BuildConfig;
import bn.poro.quran.CompressTask;
import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_quran.AddBookmarkAdapter;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.views.FontSpan;

class SajdahAdapter extends RecyclerView.Adapter<SajdahAdapter.Holder>
        implements CompressTask.CompressListener {
    final ArrayList<SajdahItem> sajdahItems;
    final MainActivity activity;
    private final float arabicFontSize, banglaFontSize;
    private final boolean justifyTrans;

    SajdahAdapter(@NonNull MainActivity activity) {
        this.activity = activity;
        sajdahItems = new ArrayList<>();
        float density = activity.getResources().getDisplayMetrics().scaledDensity;
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Activity.MODE_PRIVATE);
        justifyTrans = preferences.getBoolean(Consts.JUSTIFICATION, false);
        banglaFontSize = preferences.getInt(Consts.FONT_KEY, Consts.DEF_FONT) * density;
        arabicFontSize = preferences.getInt(Consts.ARABIC_FONT_KEY, Consts.DEF_FONT_ARABIC) * density;
        int fontId = preferences.getInt(Consts.ARABIC_FONT_FACE, 1);
        Typeface arabicFont;
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        else arabicFont = null;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        String query = "select sura,ayah,";
        if (fontId >= 5) query += "indo,";
        else query += "quran.text,";
        Cursor cursor;
        File file = new File(Utils.dataPath + "5120.db");
        if (!file.exists()) try {
            Utils.copy(activity.getAssets().open(file.getName()), file);
        } catch (Exception e) {
            L.d(e);
        }
        database.execSQL("ATTACH DATABASE ? AS trans", new String[]{file.getPath()});
        cursor = database.rawQuery(query + "content.text from quran inner join content on " +
                "content.rowid=quran.rowid where quran.text like '%۩'", null);
        while (cursor.moveToNext()) {
            String arabicText = cursor.getString(2);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(arabicText);
            int len = arabicText.length();
            if (arabicFont != null)
                stringBuilder.setSpan(new FontSpan(arabicFont), 0,
                        len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new AbsoluteSizeSpan((int) arabicFontSize, false),
                    0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.setSpan(new ForegroundColorSpan(0xff81d4fa),
                    len - 1, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.append("\n\n");
            stringBuilder.append(cursor.getString(3));
            sajdahItems.add(new SajdahItem(cursor.getInt(0),
                    cursor.getInt(1), stringBuilder));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(R.layout.ayah_subject,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SajdahItem item = sajdahItems.get(position);
        holder.ayahNo.setText(String.format("%s %d:%d",
                activity.getSuraName(item.sura), item.sura + 1, item.ayah));
        holder.textView.setText(item.text);
    }

    @Override
    public int getItemCount() {
        return sajdahItems.size();
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

        @SuppressLint("WrongConstant")
        public Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            ayahNo = itemView.findViewById(R.id.ayahNo);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, banglaFontSize);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            ayahNo.setTextSize(TypedValue.COMPLEX_UNIT_PX, arabicFontSize - 6 > banglaFontSize ?
                    arabicFontSize - 3 : banglaFontSize + 3);
            if (justifyTrans && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                textView.setTextIsSelectable(false);
                textView.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
            itemView.findViewById(R.id.bookmark).setOnClickListener(this);
            itemView.findViewById(R.id.icon).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int viewId = view.getId();
            int position = getLayoutPosition();
            if (viewId == R.id.bookmark) {
                SajdahItem item = sajdahItems.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, item.sura)
                        .putExtra(Consts.EXTRA_AYAH_NUM, item.ayah));
            } else if (viewId == R.id.icon) {
                PopupMenu popup = new PopupMenu(activity, view);
                Menu menu = popup.getMenu();
                popup.getMenuInflater().inflate(R.menu.ayah_popup2, menu);
                popup.show();
                popup.setOnMenuItemClickListener(this);
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int id = menuItem.getItemId();
            SajdahItem item = sajdahItems.get(getLayoutPosition());
            int sura = item.sura;
            int ayah = item.ayah;
            if (id == R.id.play_ayah) {
                View view = LayoutInflater.from(activity).inflate(R.layout.add_mark, null);
                RecyclerView recyclerView = view.findViewById(R.id.main_list);
                recyclerView.setLayoutManager(new MyLayoutManager(activity));
                EditText addCat = view.findViewById(R.id.addCat);
                EditText note = view.findViewById(R.id.note);
                AddBookmarkAdapter adapter = new AddBookmarkAdapter(activity,
                        String.valueOf(sura), String.valueOf(ayah), addCat, note);
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
                        SajdahAdapter.this).start();
            }
            return true;
        }
    }
}
