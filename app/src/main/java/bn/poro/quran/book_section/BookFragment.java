package bn.poro.quran.book_section;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.L;
import bn.poro.quran.views.InstantCompleteView;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class BookFragment extends Fragment implements View.OnClickListener, InitTask.Listener {
    private AlertDialog dialog;

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) inflater.getContext();
        View view = inflater.inflate(R.layout.book_main_fragment, container, false);
        dialog = new AlertDialog.Builder(activity)
                .setView(R.layout.loading)
                .setCancelable(false).show();
        new InitTask(activity, this).start();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        activity.setSupportActionBar(toolbar);
        toolbar.findViewById(R.id.delete).setOnClickListener(this);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager2);
        PageAdapter adapter = new PageAdapter(this);
        viewPager.setAdapter(adapter);
        activity.setSupportActionBar(view.findViewById(R.id.toolbar));
        new TabLayoutMediator(view.findViewById(R.id.tabs), viewPager, adapter).attach();
        return view;
    }

    @Override
    public void onDatabaseCheckFinish(SparseArray<String> books) {
        try {
            dialog.dismiss();
        } catch (Exception e) {
            L.d(e);
        }
        View view = getView();
        if (view == null) return;
        InstantCompleteView editText = view.findViewById(R.id.search);
        BookSearchAdapter searchAdapter = new BookSearchAdapter(books);
        editText.setAdapter(searchAdapter);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.book_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Activity activity = requireActivity();
        int id = item.getItemId();
        if (id == R.id.setting)
            startActivity(new Intent(activity, SettingActivity.class));
        else if (id == R.id.download) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.download_manager)
                    .setAdapter(new DownloadLIstAdapter(activity), null)
                    .setPositiveButton(DownloadService.bookQueueProgress == DownloadService.BOOK_QUEUE_STOPPED ? R.string.download_all : R.string.cancel, (dialog, which) -> activity.startService(new Intent(activity, DownloadService.class).putExtra(Consts.TYPE_KEY, DownloadService.TYPE_DOWNLOAD_ALL_BOOK))).setNeutralButton(R.string.ok, null).show();
        } else if (id == R.id.search_book) {
            startActivity(new Intent(activity, SearchBookActivity.class));
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        Activity activity = (Activity) v.getContext();
        int id = v.getId();
        if (id == R.id.delete) {
            TextView textView = activity.findViewById(R.id.search);
            textView.setText("");
            if (textView.hasFocus())
                textView.clearFocus();
            else textView.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }
}
