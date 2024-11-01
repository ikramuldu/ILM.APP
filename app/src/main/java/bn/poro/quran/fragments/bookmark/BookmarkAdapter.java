package bn.poro.quran.fragments.bookmark;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.Consts;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;

class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.MyHolder> {
    private final MainActivity activity;
    private final ArrayList<Object> items;
    private final LinearLayoutManager layoutManager;

    BookmarkAdapter(MainActivity activity, LinearLayoutManager linearLayoutManager) {
        this.activity = activity;
        layoutManager = linearLayoutManager;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        items = new ArrayList<>();
        items.add(new GroupModel(0, activity.getString(R.string.bookmark_last_read)));
        items.add(new GroupModel(1, activity.getString(R.string.menu_bookmarks)));
        Cursor cursor = database.rawQuery("select * from category where id>1", null);
        while (cursor.moveToNext()) {
            items.add(new GroupModel(cursor.getInt(0), cursor.getString(1)));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(activity).inflate(viewType == 0 ? R.layout.bookmark_group : R.layout.item_bookmark, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object obj : payloads) {
            holder.expandIcon.setRotation(0);
            holder.expandIcon.setAnimation(AnimationUtils.loadAnimation(activity, (Integer) obj));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Object object = items.get(position);
        if (object instanceof GroupModel) {
            GroupModel item = (GroupModel) object;
            if (item.expanded) holder.expandIcon.setRotation(90);
            else holder.expandIcon.setRotation(0);
            holder.textView.setText(item.name);
        } else {
            ChildModel item = (ChildModel) object;
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(activity.getSuraName(item.sura));
            stringBuilder.append(" ");
            stringBuilder.append(Utils.formatNum(item.sura + 1));
            stringBuilder.append(":");
            stringBuilder.append(Utils.formatNum(item.ayah));
            if (item.note != null && !item.note.isEmpty()) {
                stringBuilder.append("\n");
                int start = stringBuilder.length();
                stringBuilder.append(item.note);
                int end = stringBuilder.length();
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            holder.textView.setText(stringBuilder);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof GroupModel)
            return 0;
        return 1;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, DialogInterface.OnClickListener {
        private final View expandIcon;
        private final TextView textView;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            expandIcon = itemView.findViewById(R.id.icon);
            itemView.findViewById(R.id.delete).setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getLayoutPosition();
            if (view.getId() == R.id.group) expand(position);
            else if (view.getId() == R.id.delete) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(R.string.warning)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, this);
                if (expandIcon != null)
                    dialog.setMessage(activity.getString(R.string.group_del_warning, textView.getText()));
                dialog.show();
            } else {
                ChildModel childModel = (ChildModel) items.get(position);
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, childModel.sura)
                        .putExtra(Consts.EXTRA_AYAH_NUM, childModel.ayah));
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int position = getLayoutPosition();
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                    null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Object object = items.get(position);
            if (object instanceof GroupModel) {
                GroupModel item = (GroupModel) object;
                if (item.id == 0)
                    database.execSQL("delete from bookmark where category>" + item.id);
                else database.execSQL("delete from bookmark where category=" + item.id);
                if (item.expanded) expand(position);
                if (item.id > 1) {
                    database.execSQL("delete from category where id=" + item.id);
                    items.remove(position);
                    notifyItemRemoved(position);
                }
            } else {
                ChildModel item = (ChildModel) object;
                database.execSQL("delete from bookmark where rowid = " + item.id);
                items.remove(position);
                notifyItemRemoved(position);
            }
            database.close();
        }
    }

    private void expand(int position) {
        GroupModel item = (GroupModel) items.get(position);
        int nextPosition = position + 1;
        item.expanded = !item.expanded;
        if (!item.expanded) {
            notifyItemChanged(position, R.anim.rotate_back);
            int size = items.size();
            while (items.size() > nextPosition) {
                if (items.get(nextPosition) instanceof ChildModel) items.remove(nextPosition);
                else break;
            }
            notifyItemRangeRemoved(nextPosition, size - items.size());
        } else {
            notifyItemChanged(position, R.anim.rotate);
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor;
            if (item.id == 0) {
                cursor = database.rawQuery("select sura, ayah, category, rowid from bookmark where category > ? order by category desc limit 7", new String[]{String.valueOf(Consts.TIME_MIN)});
            } else {
                cursor = database.rawQuery("select bookmark.sura,bookmark.ayah,text,bookmark.rowid from bookmark left outer join note using (sura,ayah) where category=?", new String[]{String.valueOf(item.id)});
            }
            int childCount = cursor.getCount();
            if (childCount == 0)
                Toast.makeText(activity, "This folder is empty", Toast.LENGTH_SHORT).show();
            else {
                ArrayList<ChildModel> children = new ArrayList<>(childCount);
                while (cursor.moveToNext()) {
                    children.add(new ChildModel(cursor.getInt(0), cursor.getInt(1), item.id == 0 ? new SimpleDateFormat("dd MMMM, hh:mm a",MainActivity.getLocale()).format(cursor.getLong(2) * 1000) : cursor.getString(2), cursor.getInt(3)));
                }
                items.addAll(nextPosition, children);
                notifyItemRangeInserted(position + 1, childCount);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (position + childCount + 1 - layoutManager.findLastCompletelyVisibleItemPosition() > 0)
                        layoutManager.scrollToPositionWithOffset(position, 0);
                }, 300);
            }
            cursor.close();
            database.close();
        }
    }
}
