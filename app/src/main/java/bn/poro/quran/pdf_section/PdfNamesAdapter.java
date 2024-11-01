package bn.poro.quran.pdf_section;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import bn.poro.quran.media_section.CreateThumb;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_reader.PDFActivity;
import bn.poro.quran.views.MyProgressBar;

class PdfNamesAdapter extends RecyclerView.Adapter<PdfNamesAdapter.Holder> implements
        DownloadRunnable.DownloadProgressListener, CreateThumb.OnCreateThumbListener {
    private final ArrayList<PdfBookModel> books;
    final MainActivity activity;

    public void setService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    private DownloadService downloadService;

    public PdfNamesAdapter(MainActivity activity) {
        this.activity = activity;
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.FILE_LIST_DB,
                null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select id,name,writer,note from pdf", null);
        books = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            books.add(new PdfBookModel(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)));
        }
        cursor.close();
        database.close();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(activity).inflate(R.layout.islamhouse_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof DownloadRunnable)
                holder.bind((DownloadRunnable) payload);
            else if (payload instanceof String) {
                holder.progressBar.setThumb(Drawable.createFromPath((String) payload));
            }
            holder.progressBar.invalidate();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(books.get(position));
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    private int findPosition(int id) {
        for (int position = 0; position < books.size(); position++)
            if (books.get(position).id == id) return position;
        return RecyclerView.NO_POSITION;
    }

    @Override
    public void onThumbCreated(int id, String path) {
        int position = findPosition(id);
        if (position != RecyclerView.NO_POSITION)
            notifyItemChanged(position, path);
    }

    @Override
    public void onDownloadProgress(@NonNull DownloadRunnable task) {
        if (task.type != DownloadService.TYPE_PDF_BOOK) return;
        if (books.get(task.position).id != task.id) {
            int position = findPosition(task.id);
            if (position == RecyclerView.NO_POSITION) return;
            task.position = position;
        }
        notifyItemChanged(task.position, task);
    }


    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        final TextView textView, progressText;
        final MyProgressBar progressBar;
        private PdfBookModel data;

        public Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressText = itemView.findViewById(R.id.progressText);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            File file = new File(Utils.dataPath, data.id + ".pdf");
            if (!file.exists()) {
                DownloadRunnable task = null;
                if (downloadService != null)
                    task = downloadService.findTask(DownloadService.TYPE_PDF_BOOK, data.id);
                if (task == null) {
                    download();
                } else {
                    new AlertDialog.Builder(activity)
                            .setTitle(task.title)
                            .setPositiveButton(R.string.pause, (dialog, which) -> pause())
                            .show();
                }
            } else read();
        }

        private void pause() {
            activity.startService(new Intent(activity, DownloadService.class)
                    .putExtra(Consts.ID_KEY, data.id)
                    .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_PDF_BOOK));
        }

        private void read() {
            activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE)
                    .edit().putInt(Consts.LATEST_PDF, data.id)
                    .putString(Consts.LATEST_PDF_NAME, data.name).apply();
            activity.startActivity(new Intent(activity, PDFActivity.class)
                    .putExtra(Consts.ID_KEY, data.id)
                    .putExtra(Consts.LATEST_PDF_NAME, data.name));
        }

        @Override
        public boolean onLongClick(View v) {
            File file = new File(Utils.dataPath + data.id + ".pdf");
            AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(data.name)
                    .setMessage(data.note);
            if (file.exists())
                dialog.setNegativeButton(R.string.delete, (d, which) -> {
                    File file1 = new File(Utils.dataPath + data.id + ".pdf");
                    new AlertDialog.Builder(activity).setTitle(R.string.warning)
                            .setMessage(activity.getString(R.string.delete_book, data.name, DownloadRunnable.getString(file1.length())))
                            .setNegativeButton(R.string.cancel, null)
                            .setNeutralButton(R.string.delete, (dialog1, which1) -> {
                                new File(Utils.dataPath + data.id + ".pdf").delete();
                                notifyItemChanged(getLayoutPosition());
                            }).show();
                }).setPositiveButton(R.string.read, (dialog12, which) -> read());
            else
                dialog.setNegativeButton(R.string.download_question, (d, which) -> download()).setPositiveButton(R.string.okay, null);
            dialog.show();
            return true;
        }

        private void download() {
            activity.startService(new Intent(activity, DownloadService.class)
                    .putExtra(Consts.ID_KEY, data.id)
                    .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_PDF_BOOK)
                    .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + Consts.PDF_SUB_PATH + data.id + ".pdf")
                    .putExtra(Consts.PATH_KEY, Utils.dataPath + data.id + ".pdf")
                    .putExtra(Consts.NAME_KEY, data.name));
        }

        public void bind(DownloadRunnable task) {
            progressText.setText(task.getString(activity));
            switch (task.status) {
                case DownloadRunnable.STARTING:
                    progressBar.setStatus(MyProgressBar.WAITING);
                    break;
                case DownloadRunnable.COMPLETED:
                    File file = new File(Utils.dataPath + data.id + ".pdf");
                    File thumbCache = new File(activity.getCacheDir(), file.getName());
                    new CreateThumb(activity, PdfNamesAdapter.this, data.id, 0, file, thumbCache).start();
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

        void bind(PdfBookModel data) {
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
            if (downloadService != null && (task = downloadService.findTask(DownloadService.TYPE_PDF_BOOK,
                    data.id)) != null) {
                bind(task);
            } else {
                File file = new File(Utils.dataPath + this.data.id + ".pdf");
                if (file.exists()) {
                    File thumbCache = new File(activity.getCacheDir(), file.getName());
                    if (thumbCache.exists()) {
                        Drawable drawable = Drawable.createFromPath(thumbCache.getPath());
                        progressBar.setThumb(drawable);
                    } else {
                        new CreateThumb(activity, PdfNamesAdapter.this, data.id, 0, file, thumbCache).start();
                    }
                    progressBar.setStatus(MyProgressBar.COMPLETED);
                    progressText.setText(DownloadRunnable.getString(file.length()));
                } else {
                    progressBar.setStatus(MyProgressBar.NONE);
                    progressText.setText(R.string.download_question);
                }
            }
        }
    }

    private static class PdfBookModel {
        private final int id;
        private final String name, writer, note;

        public PdfBookModel(int id, String name, String writer, String note) {
            this.id = id;
            this.name = name;
            this.writer = writer;
            this.note = note;
        }
    }
}
