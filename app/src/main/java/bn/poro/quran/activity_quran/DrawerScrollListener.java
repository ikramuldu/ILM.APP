package bn.poro.quran.activity_quran;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import bn.poro.quran.Utils;

class DrawerScrollListener extends RecyclerView.OnScrollListener {
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        QuranActivity activity = (QuranActivity) Utils.getContext(recyclerView);
        if (QuranDrawerAdapter.keyboardVisible && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
            QuranDrawerAdapter.keyboardVisible = false;
        }
    }
}
