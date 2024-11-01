package bn.poro.quran.workout_section;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.R;

class WorkoutSelectionListAdapter extends RecyclerView.Adapter<WorkoutSelectionListAdapter.Holder> {
    private final WorkoutFragment fragment;

    public WorkoutSelectionListAdapter(WorkoutFragment fragment) {
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(fragment.activity).inflate(R.layout.checkbox, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.checkBox.setChecked((fragment.checkedItems & 1 << position) != 0);
        holder.checkBox.setText(fragment.title[position + 1 + position / 3]);
    }

    @Override
    public int getItemCount() {
        return WorkoutFragment.gifs.length;
    }

    public class Holder extends RecyclerView.ViewHolder implements CompoundButton.OnCheckedChangeListener {
        final CheckBox checkBox;

        public Holder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
            checkBox.setOnCheckedChangeListener(this);
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int mask = 1 << getLayoutPosition();
            if (isChecked) fragment.checkedItems |= mask;
            else fragment.checkedItems &= ~mask;
        }
    }
}
