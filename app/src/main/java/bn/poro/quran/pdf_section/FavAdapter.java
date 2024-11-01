package bn.poro.quran.pdf_section;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import bn.poro.quran.Consts;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_reader.PDFActivity;

class FavAdapter extends RecyclerView.Adapter<FavAdapter.Holder> {
    final MainActivity activity;
    private final ArrayList<ItemModel> items;

    public FavAdapter(MainActivity activity) {
        this.activity = activity;
        items = new ArrayList<>();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.bookmark, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void refresh() {
        int oldSize = items.size();
        items.clear();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = database.rawQuery("select rowid,id,time,title,position,book_name from pdf_mark order by time desc", null);
        while (cursor.moveToNext()) {
            items.add(new ItemModel(cursor.getInt(0), cursor.getInt(1), cursor.getLong(2), cursor.getString(3), cursor.getString(4), cursor.getString(5)));
        }
        cursor.close();
        database.close();
        notifyItemRangeInserted(0, items.size() - oldSize);
    }

    public class Holder extends RecyclerView.ViewHolder implements
            View.OnClickListener, DialogInterface.OnClickListener {
        public final TextView textView1;
        public final TextView textView2;
        public final TextView textView3;
        private ItemModel itemModel;

        public Holder(@NonNull View itemView) {
            super(itemView);
            textView1 = itemView.findViewById(R.id.text);
            textView2 = itemView.findViewById(R.id.text2);
            textView3 = itemView.findViewById(R.id.text3);
            itemView.setOnClickListener(this);
            itemView.findViewById(R.id.delete).setOnClickListener(this);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    Utils.dataPath + Consts.BOOKMARK_FILE, null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("delete from pdf_mark where rowid=" + itemModel.id);
            database.close();
            items.remove(getLayoutPosition());
            notifyItemRemoved(getLayoutPosition());
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.delete) {
                new AlertDialog.Builder(activity).setTitle(R.string.delete)
                        .setMessage(itemModel.subTitle)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, this).show();
            } else {
                SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME,
                        Context.MODE_PRIVATE);
                store.edit().putString(itemModel.bookName, itemModel.position).apply();
                activity.startActivity(new Intent(activity, PDFActivity.class)
                        .putExtra(Consts.LATEST_PDF_NAME, itemModel.bookName)
                        .putExtra(Consts.ID_KEY, itemModel.bookId));
            }
        }

        void bind(ItemModel item) {
            this.itemModel = item;
            String time = new SimpleDateFormat("dd MMMM hh:mm a", MainActivity.getLocale()).format(item.time);
            textView1.setText(item.bookName);
            textView2.setText(time);
            textView3.setText(item.subTitle);
        }
    }

    private static class ItemModel {
        private final int id, bookId;
        private final long time;
        private final String subTitle, bookName, position;

        public ItemModel(int id, int bookId, long time, String subTitle, String position, String bookName) {
            this.id = id;
            this.bookId = bookId;
            this.time = time;
            this.subTitle = subTitle;
            this.position = position;
            this.bookName = bookName;
        }
    }
}
