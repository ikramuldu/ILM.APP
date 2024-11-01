package bn.poro.quran.fragments.homepage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;

class HomeItemsAdapter extends RecyclerView.Adapter<HomeItemsAdapter.Holder> {
    private ArrayList<ButtonModel> items;
    private final Context context;

    HomeItemsAdapter(Context context) {
        this.context = context;
        initList();
    }

    private void initList() {
        items = new ArrayList<>();
        items.add(new ButtonRecitation());
        items.add(new ButtonQuran());
        items.add(new ButtonHadis());
        items.add(new ButtonBooks());
        items.add(new ButtonPdf());
        items.add(new ButtonMedia());
        items.add(new ButtonDictionary());
        items.add(new ButtonConcepts());
        if ("bn".equalsIgnoreCase(MainActivity.getAppLang())) {
            items.add(new ItemDua());
            items.add(new ItemSajdah());
            items.add(new ItemScience());
            items.add(new ItemNames());
            items.add(new ItemSubject());
        }
        items.add(new ItemWorkout());
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.home_grid_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ButtonModel item = items.get(position);
        holder.itemView.setOnClickListener(item);
        holder.textView.setText(item.getTitle());
        holder.textView.setCompoundDrawablesWithIntrinsicBounds(0, item.getIcon(), 0, 0);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
        }
    }
}
