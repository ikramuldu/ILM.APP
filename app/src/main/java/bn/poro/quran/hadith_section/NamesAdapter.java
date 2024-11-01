package bn.poro.quran.hadith_section;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.views.MyProgressBar;

class NamesAdapter extends RecyclerView.Adapter<NamesAdapter.MyHolder> implements DownloadRunnable.DownloadProgressListener {
    private final Context context;
    final ArrayList<HadisBookModel> books;
    private DownloadService downloadService;

    NamesAdapter(@NonNull Context context) {
        this.context = context;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,name,pub,section,hadith from hadis", null);
        books = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            books.add(new HadisBookModel(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyHolder(LayoutInflater.from(context).inflate(R.layout.islamhouse_item, parent,
                false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof DownloadRunnable) {
                holder.bind((DownloadRunnable) payload);
                holder.progressBar.invalidate();
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.bind(books.get(position));
    }

    private int findPosition(int id) {
        for (int position = 0; position < books.size(); position++)
            if (books.get(position).id == id) return position;
        return RecyclerView.NO_POSITION;
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    @Override
    public void onDownloadProgress(@NonNull DownloadRunnable task) {
        if (task.type != DownloadService.TYPE_HADIS) return;
        if (books.get(task.position).id != task.id) {
            int position = findPosition(task.id);
            if (position == RecyclerView.NO_POSITION) return;
            task.position = position;
        }
        notifyItemChanged(task.position, task);
    }

    public void setService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        final TextView textView, progressText;
        public final MyProgressBar progressBar;
        private HadisBookModel data;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressText = itemView.findViewById(R.id.progressText);
            progressBar.setOnClickListener(this);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        private void pause() {
            context.startService(new Intent(context, DownloadService.class)
                    .putExtra(Consts.ID_KEY, data.id).putExtra(Consts.TYPE_KEY, DownloadService.TYPE_CANCEL));
        }

        @Override
        public boolean onLongClick(View v) {
            File file = new File(Utils.dataPath + Consts.HADIS_SUB_PATH + data.id + ".db");
            AlertDialog.Builder dialog = new AlertDialog.Builder(context).setTitle(data.name)
                    .setMessage(data.section);
            if (file.exists())
                dialog.setNegativeButton(R.string.delete, (d, which) -> warnDelete()).setPositiveButton(R.string.read, (dialog12, which) -> read());
            else
                dialog.setNegativeButton(R.string.download_question, (d, which) -> download()).setPositiveButton(R.string.okay, null);
            dialog.show();
            return true;
        }

        @Override
        public void onClick(View view) {
            File file = new File(Utils.dataPath + Consts.HADIS_SUB_PATH, data.id + ".db");
            if (view.getId() == R.id.progressBar) {
                if (file.exists()) {
                    warnDelete();
                    return;
                }
            }
            if (!file.exists()) {
                DownloadRunnable task = null;
                if (downloadService != null)
                    task = downloadService.findTask(DownloadService.TYPE_HADIS, data.id);
                if (task == null) {
                    download();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(task.title)
                            .setPositiveButton(R.string.pause, (dialog, which) -> pause())
                            .show();
                }
            } else read();
        }

        private void warnDelete() {
            File file1 = new File(Utils.dataPath + Consts.HADIS_SUB_PATH + data.id + ".db");
            new AlertDialog.Builder(context).setTitle(R.string.warning)
                    .setMessage(context.getString(R.string.delete_book, data.name, DownloadRunnable.getString(file1.length())))
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.delete, (dialog1, which1) -> {
                        new File(Utils.dataPath + Consts.HADIS_SUB_PATH + data.id + ".db").delete();
                        notifyItemChanged(getLayoutPosition());
                    }).show();
        }

        private void download() {
            context.startService(new Intent(context, DownloadService.class)
                    .putExtra(Consts.ID_KEY, data.id)
                    .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_HADIS)
                    .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + Consts.HADIS_SUB_PATH + data.id + ".rar")
                    .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.HADIS_SUB_PATH + data.id + ".db")
                    .putExtra(Consts.PATH_KEY, Utils.dataPath + Consts.HADIS_SUB_PATH + data.id + ".rar")
                    .putExtra(Consts.NAME_KEY, data.name));
        }

        private void read() {
            context.startActivity(new Intent(context, ReadHadisActivity.class)
                    .putExtra(Consts.ID_KEY, data.id).putExtra(Consts.TITLE_KEY,data.name));
        }

        public void bind(DownloadRunnable task) {
            progressText.setText(task.getString(context));
            switch (task.status) {
                case DownloadRunnable.STARTING:
                    progressBar.setStatus(MyProgressBar.WAITING);
                    break;
                case DownloadRunnable.COMPLETED:
                    progressBar.setStatus(MyProgressBar.COMPLETED);
                    break;
                case DownloadRunnable.DOWNLOADING:
                    progressBar.setStatus(MyProgressBar.DOWNLOADING);
                    progressBar.setProgress((((float) task.totalProgress) / task.totalSize) * 360);
                    break;
                case DownloadRunnable.CANCELLED:
                    progressBar.setStatus(MyProgressBar.PARTIAL_DONE);
                    progressBar.setProgress((((float) task.totalProgress) / task.totalSize) * 360);
                    break;
                default:
                    progressBar.setStatus(MyProgressBar.NONE);
            }

        }

        void bind(HadisBookModel data) {
            this.data = data;
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(data.name);
            if (data.writer != null) {
                spannableStringBuilder.append("\n");
                int start = spannableStringBuilder.length();
                spannableStringBuilder.append(data.writer);
                spannableStringBuilder.setSpan(new StyleSpan(Typeface.ITALIC), start, spannableStringBuilder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableStringBuilder.setSpan(new RelativeSizeSpan(0.9f), start,
                        spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(spannableStringBuilder);
            DownloadRunnable task;
            if (downloadService != null && (task = downloadService.findTask(DownloadService.TYPE_HADIS, data.id)) != null) {
                bind(task);
            } else {
                File file = new File(Utils.dataPath + Consts.HADIS_SUB_PATH + this.data.id + ".db");
                if (file.exists()) {
                    progressBar.setStatus(MyProgressBar.COMPLETED);
                    progressText.setText(DownloadRunnable.getString(file.length()));
                } else {
                    progressBar.setStatus(MyProgressBar.NONE);
                    progressText.setText(R.string.download_question);
                }
            }
        }
    }

    public static class HadisBookModel {
        final int id;
        final String name;
        final String writer;
        final String section;

        public HadisBookModel(int id, String name, String publisher, String section) {
            this.id = id;
            this.name = name;
            this.writer = publisher;
            this.section = section;
        }
    }
}
