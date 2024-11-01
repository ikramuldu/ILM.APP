package bn.poro.quran.activity_search;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.R;

class SearchDrawerAdapter extends RecyclerView.Adapter<SearchDrawerAdapter.Holder> {
    private final SearchQuranActivity activity;
    private SearchTask.ResultItem[] resultItems;

    SearchDrawerAdapter(SearchQuranActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.search_drawer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SearchTask.ResultItem item = resultItems[position];
        ((TextView) holder.itemView).setText(String.format( "%s %d:%d",
                activity.suraNames[item.sura], item.sura + 1, item.ayah));
    }

    @Override
    public int getItemCount() {
        return resultItems == null ? 0 : resultItems.length;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void update(SearchTask.ResultItem[] mainCursor) {
        this.resultItems = mainCursor;
        notifyDataSetChanged();
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public Holder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            activity.layoutManager.scrollToPositionWithOffset(getLayoutPosition(), 0);
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }
}
