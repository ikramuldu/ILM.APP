package bn.poro.quran.fragments.para_list;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;

class ParaAdapter extends RecyclerView.Adapter<ParaAdapter.Holder> {
    private final MainActivity activity;
    private final ItemModel[] items;
    private Typeface arabicFont;

    ParaAdapter(MainActivity activity) {
        this.activity = activity;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("SELECT sura,ayah,text from quran where rowid in (select rowid from quran where juz is not null and ayah is not null) OR rowid in (select rowid+1 from quran where ayah is null and juz is not null) order by rowid",
                null);
        items = new ItemModel[cursor.getCount()];
        for (int i = 0; i < items.length; i++) {
            cursor.moveToPosition(i);
            items[i] = new ItemModel(cursor.getInt(0),
                    cursor.getInt(1), cursor.getString(2));
        }
        cursor.close();
        database.close();
        int fontId = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
    }

    @NonNull
    @Override
    public ParaAdapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.item_sura, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ParaAdapter.Holder holder, int position) {
        holder.number.setText(Utils.formatNum(position + 1));
        ItemModel item = items[position];
        String text = item.text;
        if (text.length() > 40) {
            text = text.substring(0, 40);
            int index = text.lastIndexOf(' ');
            if (index != -1) text = text.substring(0, index);
        }
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        stringBuilder.append(text);
        stringBuilder.append("\n");
        int start = stringBuilder.length();
        if (arabicFont != null)
            stringBuilder.setSpan(new FontSpan(arabicFont), 0, start - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.append(activity.getSuraName(item.sura));
        stringBuilder.append(" ");
        stringBuilder.append(Utils.formatNum(item.sura + 1));
        stringBuilder.append(":");
        stringBuilder.append(Utils.formatNum(item.ayah));
        int end = stringBuilder.length();
        stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.textView.setText(stringBuilder);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        final TextView number;

        Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            number = itemView.findViewById(R.id.number);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getLayoutPosition();
            activity.startActivity(new Intent(activity, QuranActivity.class)
                    .putExtra(Consts.EXTRA_SURA_ID, items[position].sura).putExtra(Consts.EXTRA_AYAH_NUM, items[position].ayah));
        }
    }
}
