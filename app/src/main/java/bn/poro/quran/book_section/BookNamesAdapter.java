package bn.poro.quran.book_section;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.BringIntoView;
import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class BookNamesAdapter extends RecyclerView.Adapter<BookNamesAdapter.MyHolder> implements DownloadRunnable.DownloadProgressListener, ServiceConnection {
    private static final int TYPE_CHILD = 1;
    private static final int TYPE_GROUP = 0;
    final Context activity;
    private final int size;
    private final ArrayList<Object> models;
    private final LinearLayoutManager layoutManager;

    BookNamesAdapter(Context activity, LinearLayoutManager linearLayoutManager) {
        this.activity = activity;
        layoutManager = linearLayoutManager;
        size = (int) (activity.getResources().getDisplayMetrics().density * 30);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from topics order by text;", null);
        models = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            models.add(new GroupModel(cursor.getInt(0), cursor.getString(1)));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(activity).inflate(viewType == TYPE_GROUP ? R.layout.text_frame : R.layout.text_icon,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof Boolean) {
                holder.textView.setCompoundDrawablesWithIntrinsicBounds((boolean) payload ? R.drawable.open_book : R.drawable.disable_book, 0, 0, 0);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        Object object = models.get(position);
        if (holder.itemView.getId() == R.id.group) {
            holder.textView.setText(((GroupModel) object).name);
        } else holder.bind((ChildModel) object);
    }

    @Override
    public int getItemViewType(int position) {
        if (models.get(position) instanceof ChildModel)
            return TYPE_CHILD;
        return TYPE_GROUP;
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    void download(String book, String name) {
        activity.startService(new Intent(activity, DownloadService.class)
                .putExtra(Consts.ID_KEY, Integer.parseInt(book))
                .putExtra(Consts.NAME_KEY, name)
                .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + Consts.BOOK_SUB_PATH + book + ".zip")
                .putExtra(Consts.PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + book+".zip")
                .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + book)
                .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_BOOK));
        Toast.makeText(activity, R.string.loading, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadProgress(DownloadRunnable task) {
        if (task.status != DownloadRunnable.COMPLETED) return;
        Object object = models.get(task.position);
        if (object instanceof GroupModel || ((ChildModel) object).id != task.id) {
            int position = find(task.id);
            if (position == -1) return;
            task.position = position;
        }
        notifyItemChanged(task.position, Boolean.TRUE);
    }

    private int find(int id) {
        for (int position = 0; position < models.size(); position++) {
            Object model = models.get(position);
            if (model instanceof ChildModel && ((ChildModel) model).id == id) return position;
        }
        return -1;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        DownloadService downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener, PopupMenu.OnMenuItemClickListener {
        final TextView textView;
        private ChildModel childModel;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            textView.setOnClickListener(this);
            if (itemView.getId() == R.id.group) {
                Drawable drawable = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_books, null);
                if (drawable == null) return;
                drawable.setBounds(0, 0, size, size);
                textView.setCompoundDrawables(drawable, null, null, null);
            } else {
                textView.setOnLongClickListener(this);
            }
        }

        @Override
        public void onClick(View view) {
            int position = getLayoutPosition();
            if (childModel == null) expand(position);
            else if (view.getId() == R.id.text) {
                String book = String.valueOf(childModel.id);
                if (new File(Utils.dataPath + Consts.BOOK_SUB_PATH, book).exists()) {
                    activity.startActivity(new Intent(activity, ReadBookActivity.class)
                            .putExtra(Consts.ID_KEY, book)
                            .putExtra(Consts.TITLE_KEY, childModel.name));
                } else download(book, childModel.name);
            }
        }

        private void showPopup(int pos) {
            File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH, String.valueOf(childModel.id));
            PopupMenu popupMenu = new PopupMenu(activity, textView);
            Menu menu = popupMenu.getMenu();
            boolean exist = file.exists();
            if (exist && (file.lastModified() / MINUTE_IN_MILLIS) < childModel.updatedTimeInMinutes)
                menu.add(pos, 1, 1, R.string.update);
            if (exist) {
                menu.add(pos, 2, 2, R.string.delete);
            } else menu.add(pos, 3, 2, R.string.download_question);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.show();
        }

        @Override
        public boolean onLongClick(View v) {
            showPopup(getLayoutPosition());
            return true;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE) {
                new File(Utils.dataPath + Consts.BOOK_SUB_PATH, String.valueOf(childModel.id)).delete();
                notifyItemChanged(getLayoutPosition(), Boolean.FALSE);
            }
        }

        @Override
        public boolean onMenuItemClick(@NonNull MenuItem item) {
            File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH, String.valueOf(childModel.id));
            switch (item.getItemId()) {
                case 1:
                case 3:
                    download(file.getName(), childModel.name);
                    return true;
                case 2:
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.warning)
                            .setMessage(activity.getString(R.string.delete_book,
                                    childModel.name, DownloadRunnable.getString(file.length())))
                            .setNegativeButton(R.string.delete, this)
                            .setPositiveButton(R.string.cancel, null)
                            .show();
                    return true;
                default:
                    return false;
            }
        }

        public void bind(ChildModel child) {
            this.childModel = child;
            File file = new File(Utils.dataPath + Consts.BOOK_SUB_PATH, String.valueOf(child.id));
            boolean dbExist = file.exists();
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(child.name);
            if (child.preloaded) stringBuilder.append(" ⭐");
            stringBuilder.append("\n");
            int start = stringBuilder.length();
            stringBuilder.setSpan(new StyleSpan(Typeface.ITALIC), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            stringBuilder.setSpan(new RelativeSizeSpan(0.9f), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            stringBuilder.append(child.writer);
            textView.setText(stringBuilder);
            textView.setCompoundDrawablesWithIntrinsicBounds(dbExist ? R.drawable.open_book : R.drawable.disable_book, 0, 0, 0);
        }
    }

    public void expand(int position) {
        GroupModel groupModel = (GroupModel) models.get(position);
        if (groupModel.childCount > 0) {
            models.subList(position + 1, position + 1 + groupModel.childCount).clear();
            notifyItemRangeRemoved(position + 1, groupModel.childCount);
            groupModel.childCount = 0;
        } else {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor = database.rawQuery("select id,text,writer,updated,serial from books where type=? order by serial;", new String[]{String.valueOf(groupModel.id)});
            ArrayList<ChildModel> childModels = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                childModels.add(new ChildModel(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4) < 1000));
            }
            cursor.close();
            database.close();
            new Handler(Looper.getMainLooper()).post(new BringIntoView(layoutManager, position + 2));
            models.addAll(position + 1, childModels);
            groupModel.childCount = childModels.size();
            notifyItemRangeInserted(position + 1, groupModel.childCount);
        }
    }

    private static class GroupModel {
        final int id;
        int childCount;
        final String name;

        public GroupModel(int id, String text) {
            this.id = id;
            this.name = text;
        }
    }

    static class ChildModel {
        final int id;
        final int updatedTimeInMinutes;
        private final boolean preloaded;
        final String name, writer;

        public ChildModel(int id, String name, String writer, int time, boolean preloaded) {
            this.id = id;
            this.name = name;
            this.writer = writer;
            this.updatedTimeInMinutes = time;
            this.preloaded = preloaded;
        }
    }
}
