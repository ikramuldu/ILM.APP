package bn.poro.quran;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MyLayoutManager extends LinearLayoutManager {
    public MyLayoutManager(Context context) {
        super(context);
    }

    @Override
    public boolean onRequestChildFocus(@NonNull RecyclerView parent, @NonNull RecyclerView.State state, @NonNull View child, @Nullable @org.jetbrains.annotations.Nullable View focused) {
        L.d("requested focus");
        return true;
    }
}
