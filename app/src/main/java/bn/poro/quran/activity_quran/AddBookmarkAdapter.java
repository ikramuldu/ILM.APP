package bn.poro.quran.activity_quran;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

public class AddBookmarkAdapter extends RecyclerView.Adapter<AddBookmarkAdapter.Holder> implements
        View.OnFocusChangeListener, TextView.OnEditorActionListener {
     final String sura, ayah;
    private final EditText addCat, noteView;
    private final ArrayList<Group> groups;
    private final ArrayList<Integer> oldCheck, newCheck;

    public AddBookmarkAdapter(Context activity, String sura, String ayah, EditText addCat, EditText note) {
        this.sura = sura;
        this.ayah = ayah;
        this.addCat = addCat;
        this.noteView = note;
        groups = new ArrayList<>();
        oldCheck = new ArrayList<>();
        newCheck = new ArrayList<>();
        groups.add(new Group(1, activity.getString(R.string.menu_bookmarks)));
        addCat.setOnFocusChangeListener(this);
        addCat.setOnEditorActionListener(this);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,name from category where id>1", null);
        while (cursor.moveToNext()) {
            groups.add(new Group(cursor.getInt(0), cursor.getString(1)));
        }
        cursor.close();
        cursor = database.rawQuery("select text from note where sura=? and ayah=?", new String[]{sura, ayah});
        if (cursor.moveToFirst()) note.setText(cursor.getString(0));
        cursor.close();
        cursor = database.rawQuery("select category from bookmark where sura=? and ayah=? and category<?", new String[]{sura, ayah, String.valueOf(Consts.TIME_MIN)});
        while (cursor.moveToNext()) oldCheck.add(cursor.getInt(0));
        newCheck.addAll(oldCheck);
        cursor.close();
        database.close();
    }


    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.checkbox, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CheckBox checkBox = (CheckBox) holder.itemView;
        Group group = groups.get(position);
        checkBox.setChecked(newCheck.contains(group.id));
        checkBox.setText(group.name);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            addCat.setText("");
            addCat.setBackgroundResource(R.drawable.edit_back);
            return;
        }
        String s = addCat.getText().toString().trim();
        addCat.setText(R.string.create_folder_title);
        addCat.setBackgroundResource(0);
        if (s.isEmpty()) return;
        int nextId = groups.get(groups.size() - 1).id + 1;
        Group group = new Group(nextId, s);
        if (groups.contains(group)) {
            Toast.makeText(v.getContext(), "folder already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        newCheck.add(nextId);
        groups.add(group);
        notifyItemInserted(groups.size() - 1);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("insert into category(id,name) values(?,?)", new Object[]{nextId, s});
        database.close();
    }

    public boolean save() {
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        boolean checked = oldCheck.isEmpty() && !newCheck.isEmpty();
        String note = noteView.getText().toString();
        if (note.isEmpty())
            database.execSQL("delete from note where sura=? and ayah=?", new String[]{sura, ayah});
        else database.execSQL("insert into note values(?,?,?)", new String[]{sura, ayah, note});
        for (int id : newCheck) {
            if (oldCheck.remove((Object) id)) continue;
            database.execSQL("insert into bookmark values(?,?,?)", new String[]{sura, ayah, String.valueOf(id)});
        }
        for (int id : oldCheck) {
            database.execSQL("delete from bookmark where sura=? and ayah=? and category=?", new String[]{sura, ayah, String.valueOf(id)});
        }
        database.close();
        return checked;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        v.clearFocus();
        return false;
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public Holder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = groups.get(getLayoutPosition()).id;
            if (newCheck.contains(id)) newCheck.remove((Object) id);
            else newCheck.add(id);
        }
    }

    private static class Group {
        final int id;
        final String name;

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Group && name.equals(((Group) obj).name);
        }

        public Group(int id, String name) {

            this.id = id;
            this.name = name;
        }
    }
}
