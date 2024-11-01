package bn.poro.quran.fragments.sura_list;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.activity_setting.SettingActivity;

class SuraNamesAdapter extends RecyclerView.Adapter<SuraNamesAdapter.Holder> {
    private final MainActivity activity;
    private int[][] ints;
    private final String[] trans;

    SuraNamesAdapter(MainActivity activity) {
        this.activity = activity;
        try {
            retry();
        } catch (Exception e) {
            try {
                Utils.copyFromAssets(activity, Consts.QURAN_DB_NAME);
            } catch (Exception ex) {
                L.d(ex);
            }
            retry();
        }
        trans = activity.getResources().getStringArray(R.array.sura_translation);
    }

    private void retry() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.QURAN_DB_NAME,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select text,word from quran where ayah is null", null);
        ints = new int[cursor.getCount()][2];
        for (int i = 0; i < ints.length; i++) {
            cursor.moveToPosition(i);
            ints[i][0] = cursor.getInt(0);
            ints[i][1] = cursor.getInt(1);
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(R.layout.item_sura, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(activity.getSuraName(position));
        stringBuilder.append(", ");
        stringBuilder.append(activity.getString(R.string.quran_ayah));
        stringBuilder.append(": ");
        stringBuilder.append(Utils.formatNum(ints[position][0]));
        stringBuilder.append("\n");
        int start = stringBuilder.length();
        stringBuilder.append(trans[position]);
        stringBuilder.append(", ");
        if (ints[position][1] == 0) {
            stringBuilder.append(activity.getString(R.string.makki));
        } else {
            stringBuilder.append(activity.getString(R.string.madani));
        }
        stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.number.setText(Utils.formatNum(position + 1));
        holder.textView.setText(stringBuilder);
    }

    @Override
    public int getItemCount() {
        return ints.length;
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
            activity.startActivity(new Intent(activity, QuranActivity.class)
                    .putExtra(Consts.EXTRA_SURA_ID, getLayoutPosition()));
        }
    }
}
