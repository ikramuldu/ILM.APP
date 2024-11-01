package bn.poro.quran.fragments.learning_list;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.fragments.alphabet.AlphabetFragment;
import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;


class LearningListAdapter extends RecyclerView.Adapter<LearningListAdapter.MyHolder> {
    private final String[][] strings;


    LearningListAdapter() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from page0", null);
        strings = new String[cursor.getCount()][2];
        for (int i = 0; i < strings.length; i++) {
            cursor.moveToPosition(i);
            strings[i][0] = cursor.getString(0);
            strings[i][1] = cursor.getString(1);
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_arabic, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.textView.setText(strings[position][1]);
    }

    @Override
    public int getItemCount() {
        return strings.length;
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String[] s = strings[getLayoutPosition()];
            Fragment fragment = new AlphabetFragment();
            Bundle bundle = new Bundle();
            bundle.putString("table", s[0]);
            bundle.putString("title", s[1]);
            fragment.setArguments(bundle);
            Utils.replaceFragment((FragmentActivity) Utils.getContext(view), fragment);
        }
    }
}

