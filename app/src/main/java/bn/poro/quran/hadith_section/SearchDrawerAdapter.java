package bn.poro.quran.hadith_section;

import android.app.Activity;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.L;
import bn.poro.quran.R;

public class SearchDrawerAdapter extends RecyclerView.Adapter<SearchDrawerAdapter.Holder> {
    private LinearLayoutManager layoutManager;
    private Cursor cursor;

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.search_drawer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        cursor.moveToPosition(position);
        if (cursor.getString(1) == null)
            ((TextView) holder.itemView).setText(holder.itemView.getContext().getString(R.string.hadis_no, cursor.getInt(0)));
        else ((TextView) holder.itemView).setText(cursor.getString(2));
    }

    @Override
    public int getItemCount() {
        L.d("get count");
        return cursor == null ? 0 : cursor.getCount();
    }

    public void update(LinearLayoutManager layoutManager, Cursor cursor) {
        this.layoutManager = layoutManager;
        if (this.cursor != null) notifyItemRangeRemoved(0, this.cursor.getCount());
        this.cursor = cursor;
        notifyItemRangeInserted(0, cursor.getCount());
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public Holder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            layoutManager.scrollToPositionWithOffset(getLayoutPosition(), 0);
            DrawerLayout drawerLayout = ((Activity) v.getContext()).findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
