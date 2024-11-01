package bn.poro.quran.fragments.understand;

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

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.fragments.noun.NounFragment;
import bn.poro.quran.fragments.root.WordFragment;


class UnderstandListAdapter extends RecyclerView.Adapter<UnderstandListAdapter.MyHolder> {
    private final String[] strings;

    UnderstandListAdapter() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from page4", null);
        strings = new String[cursor.getCount()];
        for (int i = 0; i < strings.length; i++) {
            cursor.moveToPosition(i);
            strings[i] = cursor.getString(1);
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
        holder.textView.setText(strings[position]);
    }

    @Override
    public int getItemCount() {
        return strings.length;
    }

    static class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Fragment fragment;
            Bundle arg = new Bundle();
            switch (getLayoutPosition()) {
                case 0:
                    fragment = new NounFragment();
                    arg.putString("table", "noun");
                    break;
                case 1:
                    fragment = new NounFragment();
                    arg.putString("table", "pronoun");
                    break;
                case 2:
                    fragment = new NounFragment();
                    arg.putString("table", "proper_noun");
                    break;
                default:
                    fragment = new WordFragment();
            }
            arg.putString("title", textView.getText().toString());
            fragment.setArguments(arg);
            Utils.replaceFragment((FragmentActivity) Utils.getContext(view), fragment);
        }
    }
}

