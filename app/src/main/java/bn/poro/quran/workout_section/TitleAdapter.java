package bn.poro.quran.workout_section;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import bn.poro.quran.R;

class TitleAdapter extends RecyclerView.Adapter<TitleAdapter.Holder> {
    final WorkoutFragment fragment;

    public TitleAdapter(WorkoutFragment workoutFragment) {
        fragment = workoutFragment;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(fragment.getLayoutInflater().inflate(viewType == 0 ?
                R.layout.item_head : R.layout.item_title, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder.imageView != null) {
            int index = position - 1 - position / 4;
            Glide.with(fragment).load(WorkoutFragment.gifs[index]).into(holder.imageView);
            if((WorkoutFragment.STARRED_ITEMS & 1 << index) != 0){
                holder.textView.setText(fragment.title[position]+" ⭐");
                return;
            }
        }
        holder.textView.setText(fragment.title[position]);
    }

    @Override
    public int getItemViewType(int position) {
        if (position % 4 == 0) return 0;
        return 1;
    }

    @Override
    public int getItemCount() {
        return fragment.title.length;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        ImageView imageView;

        Holder(@NonNull View itemView) {
            super(itemView);
            if (itemView.getId() == R.id.text) textView = (TextView) itemView;
            else {
                textView = itemView.findViewById(R.id.text);
                imageView = itemView.findViewById(R.id.image);
                itemView.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                ((AlertDialog) v.getTag()).dismiss();
                return;
            }
            View alertView = LayoutInflater.from(fragment.activity).inflate(R.layout.alert_single, null);
            View view = alertView.findViewById(R.id.close_button);
            view.setOnClickListener(this);
            int p = getLayoutPosition();
            ((TextView) alertView.findViewById(R.id.headline)).setText(textView.getText());
            ((TextView) alertView.findViewById(R.id.text)).setText(WorkoutFragment.workoutDetails[p - 1 - p / 4]);
            Glide.with(fragment).load(WorkoutFragment.gifs[p - 1 - p / 4]).into((ImageView) alertView.findViewById(R.id.image));
            view.setTag(new AlertDialog.Builder(fragment.activity)
                    .setView(alertView)
                    .show());
        }
    }
}
