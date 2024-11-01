package bn.poro.quran.activity_quran;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Consts;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class QuranDrawerAdapter extends RecyclerView.Adapter<QuranDrawerAdapter.MyHolder> {
    static boolean keyboardVisible;
    private final QuranActivity activity;

    QuranDrawerAdapter(QuranActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(activity).inflate(R.layout.item_drawer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Utils.formatNum(position + 1));
        stringBuilder.append(". ");
        stringBuilder.append(QuranActivity.allSuraInfo.get(position).name);
        holder.textView.setText(stringBuilder);
        holder.total.setText("/" + QuranActivity.allSuraInfo.get(position).totalAyah);
        holder.editText.setText("");
    }

    @Override
    public int getItemCount() {
        return Consts.SURA_COUNT;
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextView.OnEditorActionListener, View.OnFocusChangeListener {
        final TextView textView;
        final TextView total;
        final EditText editText;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            total = itemView.findViewById(R.id.total);
            editText = itemView.findViewById(R.id.edit);
            itemView.setOnClickListener(this);
            total.setOnClickListener(this);
            editText.setOnFocusChangeListener(this);
            editText.setOnEditorActionListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view instanceof ViewGroup) {
                go(0);
                return;
            }
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm == null) return;
            imm.showSoftInput(editText, 0);
            keyboardVisible = true;
        }

        private void go(int ayah) {
            int position = getLayoutPosition();
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
            activity.jumpTo(position,ayah);
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            if (v.getText().length() == 0) {
                Toast.makeText(activity, R.string.enter_num, Toast.LENGTH_SHORT).show();
                return true;
            }
            int ayah = Integer.parseInt(v.getText().toString());
            int totalAyah = QuranActivity.allSuraInfo.get(getLayoutPosition()).totalAyah;
            if (ayah <= totalAyah) {
                go(ayah);
                v.setText("");
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm == null) return false;
                imm.hideSoftInputFromWindow(activity.findViewById(R.id.edit).getWindowToken(), 0);
                keyboardVisible = false;
            } else {
                Toast.makeText(activity, "মোট আয়াত " + totalAyah, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) keyboardVisible = true;
        }
    }
}
