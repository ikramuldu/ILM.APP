package bn.poro.quran.fragments.noun;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class NounAdapter extends RecyclerView.Adapter<NounAdapter.Holder> {
    private final String[][] strings;
    private final Activity activity;

    NounAdapter(Activity activity, String table) {
        this.activity = activity;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select ar,bn,en from " + table, null);
        strings = new String[cursor.getCount()][cursor.getColumnCount()];
        for (int i = 0; i < strings.length; i++) {
            cursor.moveToPosition(i);
            for (int j = 0; j < strings[i].length; j++)
                strings[i][j] = cursor.getString(j);
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(viewType == 0 ? R.layout.noun_header : R.layout.item_noun, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.arabic.setText(strings[position][0]);
        holder.bangla.setText(strings[position][1]);
        holder.english.setText(strings[position][2]);
    }

    @Override
    public int getItemCount() {
        return strings.length;
    }

    public static class Holder extends RecyclerView.ViewHolder {
        final TextView arabic;
        final TextView bangla;
        final TextView english;

        public Holder(@NonNull View itemView) {
            super(itemView);
            arabic = itemView.findViewById(R.id.arabic);
            bangla = itemView.findViewById(R.id.bangla);
            english = itemView.findViewById(R.id.english);
        }
    }
}
