package bn.poro.quran.book_section;

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
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;

class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.MyHolder> {
    private static final String BOOKMARK_QUERY = "select book_mark.rowid,book,books.text,position,`offset`,time,book_mark.text from book_mark left outer join books on book=id where time>? order by time desc";
    private final Context activity;
    private final ArrayList<Bookmark> bookmarks;

    BookmarkAdapter(@NonNull Context activity) {
        this.activity = activity;
        bookmarks = new ArrayList<>();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(activity).inflate(R.layout.bookmark, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Bookmark bookmark = bookmarks.get(position);
        String time = new SimpleDateFormat("dd MMMM hh:mm a", MainActivity.getLocale()).format(bookmark.time);
        holder.timeText.setText(time);
        holder.bookName.setText(bookmark.bookName);
        holder.detailText.setText(bookmark.chapterName);
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public void refresh() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        long latestTime;
        if (!bookmarks.isEmpty()) {
            latestTime = bookmarks.get(0).time;
        } else latestTime = 0;
        database.execSQL("ATTACH DATABASE ? AS b;", new String[]{Utils.dataPath + Consts.FILE_LIST_DB});
        Cursor cursor = database.rawQuery(BOOKMARK_QUERY, new String[]{String.valueOf(latestTime)});
        int count = cursor.getCount();
        if (count > 0) {
            ArrayList<Bookmark> temp = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                cursor.moveToPosition(i);
                temp.add(new Bookmark(cursor));
            }
            bookmarks.addAll(0, temp);
            notifyItemRangeInserted(0, count);
        }
        cursor.close();
        database.close();
    }

    class MyHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, DialogInterface.OnClickListener, View.OnLongClickListener {
        public final TextView bookName;
        public final TextView timeText;
        public final TextView detailText;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            bookName = itemView.findViewById(R.id.text);
            timeText = itemView.findViewById(R.id.text2);
            detailText = itemView.findViewById(R.id.text3);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            itemView.findViewById(R.id.delete).setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Bookmark bookmark = bookmarks.get(getLayoutPosition());
            if (view.getId() == R.id.delete) {
                new AlertDialog.Builder(activity).setTitle(R.string.delete)
                        .setMessage(bookmark.chapterName)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.ok, this).show();
                return;
            }
            SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
            store.edit().putInt(Consts.BOOK_POSITION_KEY + bookmark.bookID, bookmark.position)
                    .putInt(Consts.BOOK_OFFSET_KEY + bookmark.bookID, bookmark.offset).apply();
            activity.startActivity(new Intent(activity, ReadBookActivity.class)
                    .putExtra(Consts.ID_KEY, String.valueOf(bookmark.bookID))
                    .putExtra(Consts.TITLE_KEY, bookmark.bookName));
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = getLayoutPosition();
            Bookmark bookmark = bookmarks.get(position);
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    Utils.dataPath + Consts.BOOKMARK_FILE, null,
                    SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            database.execSQL("delete from book_mark where rowid=" + bookmark.id);
            bookmarks.remove(position);
            notifyItemRemoved(position);
            database.close();
        }

        @Override
        public boolean onLongClick(View v) {
            Bookmark bookmark = bookmarks.get(getLayoutPosition());
            new AlertDialog.Builder(activity).setTitle(R.string.delete)
                    .setMessage(bookmark.chapterName)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, this).show();
            return false;
        }
    }

    private static class Bookmark {

        final int id, bookID, position, offset;
        final String bookName, chapterName;
        final long time;

        public Bookmark(@NonNull Cursor cursor) {//bookmark.rowid,book,name,position,`offset`,time,text
            id = cursor.getInt(0);
            bookID = cursor.getInt(1);
            bookName = cursor.getString(2);
            position = cursor.getInt(3);
            offset = cursor.getInt(4);
            time = cursor.getLong(5);
            chapterName = cursor.getString(6);
        }
    }
}
