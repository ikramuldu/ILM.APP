package bn.poro.quran.book_section;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.BringIntoView;
import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.Holder> {
    final ArrayList<DrawerItem> drawerItems;
    final ReadBookActivity activity;
    private final LinearLayoutManager layoutManager;

    DrawerAdapter(@NonNull ReadBookActivity activity, LinearLayoutManager layoutManager) {
        this.activity = activity;
        this.layoutManager = layoutManager;
        drawerItems = new ArrayList<>();
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOK_SUB_PATH + activity.bookID, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,text from content where type=1", null);
        while (cursor.moveToNext()) {
            DrawerItem item = new DrawerItem(cursor, 1);
            drawerItems.add(item);
        }
        cursor.close();
        new CheckExpandabilityTask(this, 0, drawerItems.size()).start();
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof Integer)
                holder.icon.setAnimation(AnimationUtils.loadAnimation(activity, (int) payload));
            else holder.icon.setImageResource(R.drawable.expand);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(
                R.layout.book_drawer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DrawerItem item = drawerItems.get(position);
        holder.icon.setImageResource(
                item.nextType == 0 ? R.drawable.ic_dot : R.drawable.expand);
        holder.icon.setRotation(item.isExpanded ? 90 : 0);
        holder.itemView.setPadding(((item.level - 1) * holder.title.getPaddingBottom()),
                0, 0, 0);
        holder.title.setText(item.title);
    }

    @Override
    public int getItemCount() {
        return drawerItems.size();
    }


    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;
        final ImageView icon;

        public Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            expand(getLayoutPosition());
        }
    }

    private void expand(int position) {
        DrawerItem selectedItem = drawerItems.get(position);
        int nextPosition = position + 1;
        if (selectedItem.isExpanded) {
            int removed = 0;
            while (true) {
                DrawerItem nextItem = drawerItems.get(nextPosition);
                if (nextItem.level > selectedItem.level) {
                    drawerItems.remove(nextPosition);
                    removed++;
                } else break;
            }
            selectedItem.isExpanded = false;
            notifyItemChanged(position, R.anim.rotate_back);
            notifyItemRangeRemoved(nextPosition, removed);
            return;
        }
        if (selectedItem.nextType == 0) {
            activity.layoutManager.scrollToPositionWithOffset(selectedItem.id, 0);
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        int nextId;
        if (nextPosition < getItemCount()) {
            nextId = drawerItems.get(nextPosition).id;
        } else nextId = Integer.MAX_VALUE;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOK_SUB_PATH
                + activity.bookID, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery(
                "select id,text from content where id between ? and ? and type=?",
                new String[]{String.valueOf(selectedItem.id + 1),
                        String.valueOf(nextId - 1)
                        , String.valueOf(selectedItem.nextType)});
        int count = cursor.getCount();
        selectedItem.isExpanded = true;
        notifyItemChanged(position, R.anim.rotate);
        ArrayList<DrawerItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            DrawerItem item = new DrawerItem(cursor, selectedItem.level + 1);
            items.add(item);
        }
        new Handler(Looper.getMainLooper()).post(new BringIntoView(layoutManager, nextPosition + 1));
        drawerItems.addAll(nextPosition, items);
        new CheckExpandabilityTask(this, nextPosition, nextPosition + count).start();
        notifyItemRangeInserted(nextPosition, count);
        cursor.close();
        database.close();
    }
}
