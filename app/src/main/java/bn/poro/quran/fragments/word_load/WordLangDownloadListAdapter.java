package bn.poro.quran.fragments.word_load;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import bn.poro.quran.L;
import bn.poro.quran.RecreateManager;
import bn.poro.quran.views.MyProgressBar;
import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.Utils;

public class WordLangDownloadListAdapter extends
        RecyclerView.Adapter<WordLangDownloadListAdapter.Holder> implements
        DownloadRunnable.DownloadProgressListener, ServiceConnection {
    public ItemModel[] itemModels;
    public SettingActivity activity;
    public DownloadService downloadService;

    public WordLangDownloadListAdapter() {
    }

    public WordLangDownloadListAdapter(SettingActivity activity) {
        this();
        this.activity = activity;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        database.execSQL("ATTACH DATABASE ? AS sts", new String[]{Utils.dataPath + Consts.BOOKMARK_FILE});
        Cursor cursor = database.rawQuery("SELECT word.id,name,version,ver,extra,size from word left outer join sts.status using (id) order by ver desc,name", null);
        itemModels = new ItemModel[cursor.getCount()];
        for (int i = 0; i < itemModels.length; i++) {
            cursor.moveToPosition(i);
            itemModels[i] = new ItemModel(cursor.getInt(0), Utils.getLanguageName(cursor.getString(1)), cursor.getInt(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(R.layout.item_check, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        ItemModel itemModel = itemModels[position];
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads)
            if (payload instanceof Integer) {
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder(itemModel.displayName);
                stringBuilder.append("\n");
                int start = stringBuilder.length();
                int progress = (int) payload;
                stringBuilder.append(DownloadRunnable.getString(progress));
                stringBuilder.append("/");
                stringBuilder.append(DownloadRunnable.getString(itemModel.fileSize));
                int end = stringBuilder.length();
                stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.checkbox.setText(stringBuilder);
                holder.downloadButton.setStatus(MyProgressBar.DOWNLOADING);
                holder.downloadButton.setProgress((((float) progress) / itemModel.fileSize) * 360);
                holder.downloadButton.invalidate();
            } else {
                boolean completed = (boolean) payload;
                holder.checkbox.setEnabled(completed);
                holder.downloadButton.setStatus(completed ? MyProgressBar.COMPLETED : MyProgressBar.NONE);
                holder.downloadButton.invalidate();
            }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ItemModel itemModel = itemModels[position];
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(itemModel.displayName);
        stringBuilder.append("\n");
        int start = stringBuilder.length();
        File file = new File(Utils.dataPath + itemModel.id + ".db");
        if (file.exists())
            stringBuilder.append(DownloadRunnable.getString(file.length()));
        else {
            File tempFile = new File(Utils.dataPath + itemModel.id + ".zip" + Consts.LOADING_FILE);
            if (tempFile.exists()) {
                stringBuilder.append(DownloadRunnable.getString(tempFile.length()));
                stringBuilder.append("/");
            }
            stringBuilder.append(DownloadRunnable.getString(itemModel.fileSize));
        }
        int end = stringBuilder.length();
        stringBuilder.setSpan(new ForegroundColorSpan(activity.secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.checkbox.setText(stringBuilder);
        boolean loading = downloadService != null && downloadService.findTask(DownloadService.TYPE_TRANSLATION_AND_WORD, itemModel.id) != null;
        holder.downloadButton.setVisibility(itemModel.id == 0 ? View.GONE : View.VISIBLE);
        if (itemModel.deviceVersion == 0) {
            if (loading) {
                holder.downloadButton.setStatus(MyProgressBar.WAITING);
            } else {
                holder.downloadButton.setStatus(MyProgressBar.NONE);
            }
            holder.checkbox.setChecked(false);
            holder.checkbox.setEnabled(false);
        } else {
            holder.checkbox.setEnabled(true);
            holder.checkbox.setChecked(itemModel.selected);
            if (itemModel.deviceVersion >= itemModel.version) {
                holder.downloadButton.setStatus(MyProgressBar.COMPLETED);
            } else {
                if (loading) {
                    holder.downloadButton.setStatus(MyProgressBar.WAITING);
                } else {
                    holder.downloadButton.setStatus(MyProgressBar.UPDATE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemModels == null ? 0 : itemModels.length;
    }

    int getItem(int id) {
        for (int position = 0; position < itemModels.length; position++) {
            if (id == itemModels[position].id) return position;
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void onDownloadProgress(@NonNull DownloadRunnable task) {
        int position = getItem(task.id);
        if (position == RecyclerView.NO_POSITION) return;
        if (task.status == DownloadRunnable.STARTING) return;
        if (task.status == DownloadRunnable.DOWNLOADING) {
            notifyItemChanged(position, task.totalProgress);
            return;
        }
        boolean completed = task.status == DownloadRunnable.COMPLETED;
        if (completed) {
            ItemModel itemModel = itemModels[position];
            itemModel.deviceVersion = itemModel.version;
        }
        notifyItemChanged(position, completed);
    }

    public void bindService() {
        activity.bindService(new Intent(activity, DownloadService.class), this, Context.BIND_ABOVE_CLIENT);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        downloadService = ((DownloadService.MyBinder) service).getService();
        downloadService.setListener(this);
        L.d("service connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        downloadService = null;
    }

    public void unBindService() {
        if (downloadService != null) {
            downloadService.setListener(null);
            activity.unbindService(this);
        }
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, DialogInterface.OnClickListener {
        private final CompoundButton checkbox;
        private final MyProgressBar downloadButton;

        public Holder(@NonNull View itemView) {
            super(itemView);
            checkbox = itemView.findViewById(R.id.text);
            downloadButton = itemView.findViewById(R.id.progressBar);
            checkbox.setOnClickListener(this);
            downloadButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getLayoutPosition();
            ItemModel itemModel = itemModels[position];
            if (v.getId() == R.id.text) {
                SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                database.execSQL("update status set extra =? where id=?", new String[]{itemModel.selected ? "0" : "1", String.valueOf(itemModel.id)});
                database.close();
                RecreateManager.recreateAll();
                itemModel.selected = !itemModel.selected;
                return;
            } else if (downloadButton.status == MyProgressBar.COMPLETED) {
                new AlertDialog.Builder(activity).setTitle(R.string.warning)
                        .setMessage(activity.getString(R.string.delete) + " '" + itemModel.displayName + "'?")
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.delete, this)
                        .show();
                return;
            } else if (downloadButton.status == MyProgressBar.DOWNLOADING || downloadButton.status == MyProgressBar.WAITING) {
                downloadButton.setStatus(MyProgressBar.NONE);
                Toast.makeText(activity, R.string.canceling, Toast.LENGTH_SHORT).show();
            } else if (downloadButton.status == MyProgressBar.NONE || downloadButton.status == MyProgressBar.UPDATE) {
                Toast.makeText(activity, R.string.downloading_title, Toast.LENGTH_SHORT).show();
                downloadButton.setStatus(MyProgressBar.WAITING);
            }
            downloadButton.invalidate();
            activity.startService(new Intent(activity, DownloadService.class)
                    .putExtra(Consts.ID_KEY, itemModel.id)
                    .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_TRANSLATION_AND_WORD)
                    .putExtra(Consts.NAME_KEY, itemModel.displayName)
                    .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + "zip/" + itemModel.id + ".zip")
                    .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + itemModel.id + ".db")
                    .putExtra(Consts.PATH_KEY, Utils.dataPath + itemModel.id + ".zip"));
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            ItemModel itemModel = itemModels[getLayoutPosition()];
            if (which == DialogInterface.BUTTON_POSITIVE) {
                new File(Utils.dataPath + itemModel.id + ".db").delete();
                SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.BOOKMARK_FILE,
                        null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                database.execSQL("delete from status where id=" + itemModel.id);
                database.close();
                itemModel.selected = false;
                itemModel.deviceVersion = 0;
                notifyItemChanged(getLayoutPosition(), false);
            }
        }
    }

    public static class ItemModel {
        final int id, version, fileSize;
        int deviceVersion;
        boolean selected;
        final String displayName;

        public ItemModel(int id, String name, int version, int deviceVersion, int extra, int fileSize) {
            this.id = id;
            this.version = version;
            this.deviceVersion = deviceVersion;
            this.selected = extra == 1;
            this.fileSize = fileSize;
            this.displayName = name;
        }
    }
}
