package bn.poro.quran.media_section;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.multidex.BuildConfig;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_reader.PDFActivity;
import bn.poro.quran.views.MyProgressBar;

class DownloadListAdapter extends RecyclerView.Adapter<DownloadListAdapter.Holder> implements DownloadRunnable.DownloadProgressListener, CreateThumb.OnCreateThumbListener {
    private static final int URL_INDEX = 5;
    private static final int DESC_INDEX = 4;
    private static final int SIZE_INDEX = 3;
    private static final String[] types = {"books", "articles", "videos", "audios", "fatwa"};
    private static final String[] extensionOf = {".pdf", ".pdf", ".mp4", ".mp3", ".pdf"};
    private static final int WRITER_INDEX = 2;
    private static final int NAME_INDEX = 1;
    private static final int ID_INDEX = 0;
    private final MediaHomeActivity activity;
    private final DownloadItem[] downloadItems;
    final int type;
    private DownloadService downloadService;

    DownloadListAdapter(MediaHomeActivity activity, int type) {
        int sort = activity.sortBy;
        String select = "select id,name,author,size,`desc`,url from data where type=? order by ";
        String orderBy;
        if (sort == R.id.large_small) {
            orderBy = "size desc";
        } else if (sort == R.id.small_large) {
            orderBy = "size";
        } else if (sort == R.id.new_old) {
            orderBy = "date desc";
        } else if (sort == R.id.old_new) {
            orderBy = "date";
        } else if (sort == R.id.z_a) {
            orderBy = "name desc";
        } else {
            orderBy = "name";
        }

        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + MediaHomeActivity.lang + ".db", null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery(select + orderBy, new String[]{String.valueOf(type)});
        downloadItems = new DownloadItem[cursor.getCount()];
        for (int i = 0; i < downloadItems.length; i++) {
            cursor.moveToPosition(i);
            downloadItems[i] = new DownloadItem(cursor.getInt(ID_INDEX),
                    cursor.getString(NAME_INDEX),
                    cursor.getString(WRITER_INDEX),
                    cursor.getLong(DESC_INDEX),
                    cursor.getString(SIZE_INDEX),
                    cursor.getString(URL_INDEX));
        }
        cursor.close();
        database.close();
        this.type = type;
        this.activity = activity;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(R.layout.islamhouse_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object payload : payloads) {
            if (payload instanceof String) {
                Drawable drawable = Drawable.createFromPath((String) payload);
                holder.progressBar.setThumb(drawable);
                holder.progressBar.invalidate();
            } else if (payload instanceof DownloadRunnable) {
                holder.bind((DownloadRunnable) payload);
                holder.progressBar.invalidate();
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        DownloadItem downloadItem = downloadItems[position];
        holder.bind(downloadItem);
    }

    static String getFileName(String id, int type) {
        return id + extensionOf[type];
    }

    @Override
    public int getItemCount() {
        return downloadItems.length;
    }

    @Override
    public void onDownloadProgress(DownloadRunnable task) {
        if (task.type != type) return;
        if (task.id != downloadItems[task.position].id) {
            int position = getPosition(task.id);
            if (position == -1) return;
            task.position = position;
        }
        if (downloadItems[task.position].size == 0) {
            downloadItems[task.position].size = task.totalSize;
        }
        notifyItemChanged(task.position, task);
    }

    private int getPosition(int id) {
        for (int i = 0; i < downloadItems.length; i++) {
            if (id == downloadItems[i].id) return i;
        }
        return -1;
    }

    @Override
    public void onThumbCreated(int id, String path) {
        int position = getPosition(id);
        if (position >= 0)
            notifyItemChanged(position, path);
    }

    public void setService(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener, DialogInterface.OnClickListener {
        final TextView textView, progressText;
        final MyProgressBar progressBar;
        private DownloadItem data;

        Holder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            progressBar = itemView.findViewById(R.id.progressBar);
            progressText = itemView.findViewById(R.id.progressText);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            progressBar.setOnClickListener(this);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            String fileName = getFileName(String.valueOf(data.id), type);
            File file = new File(Utils.dataPath + fileName);
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    file.delete();
                    new File(Utils.dataPath, fileName).delete();
                    notifyItemChanged(getLayoutPosition());
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    new AlertDialog.Builder(activity).setTitle("Delete File?")
                            .setMessage(fileName + " (" + DownloadRunnable.getString(file.length()) + ")")
                            .setPositiveButton("Delete", this)
                            .setNeutralButton("Cancel", null).show();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{activity.getString(R.string.mail)});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Islamhouse.com/" + MediaHomeActivity.lang
                            + "/" + types[type] + "/" + data.id);
                    intent.putExtra(Intent.EXTRA_TEXT, fileName);
                    activity.startActivity(intent);
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            String ext = extensionOf[type];
            File file = new File(Utils.dataPath + getFileName(
                    String.valueOf(data.id), type));
            if (file.exists()) {
                if (type == 3 || type == 2) {
                    play(file.getPath(), data.name);
                } else if (ext.equals(".pdf"))
                    activity.startActivity(new Intent(activity, PDFActivity.class)
                            .putExtra(Consts.ID_KEY, data.id)
                            .putExtra(Consts.LATEST_PDF_NAME, data.name));
                else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            ext.substring(1));
                    intent.setDataAndType(FileProvider.getUriForFile(activity,
                            BuildConfig.APPLICATION_ID + ".provider", file), mimeType);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        activity.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, "No viewer found for " + ext,
                                Toast.LENGTH_LONG).show();
                    }
                }
            } else if (v.getId() == R.id.progressBar || ext.equals(".pdf")) {//file not exist
                activity.startService(new Intent(activity, DownloadService.class)
                        .putExtra(Consts.ID_KEY, data.id)
                        .putExtra(Consts.TYPE_KEY, type)
                        .putExtra(Consts.URL_KEY, data.url)
                        .putExtra(Consts.PATH_KEY, Utils.dataPath + data.id + extensionOf[type])
                        .putExtra(Consts.NAME_KEY, data.name));
            } else if (type == 3 || type == 2) {
                play(data.url, data.name);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            int start = 0;
            stringBuilder.append(data.name);
            stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.append("\n\n");
            if (data.author != null) {
                start = stringBuilder.length();
                stringBuilder.append("Writer: ");
                stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.append(data.author);
                stringBuilder.append("\n");
            }
            if (data.desc != null) {
                stringBuilder.append("\n");
                stringBuilder.append(data.desc);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity).setMessage(stringBuilder)
                    .setNeutralButton("Report", this);
            if (new File(Utils.dataPath + getFileName(String.valueOf(data.id), type)).exists())
                builder.setNegativeButton("Delete", this);
            builder.show();
            return true;
        }

        public void bind(DownloadRunnable task) {
            progressText.setText(task.getString(activity));
            switch (task.status) {
                case DownloadRunnable.STARTING:
                    progressBar.setStatus(MyProgressBar.WAITING);
                    break;
                case DownloadRunnable.COMPLETED:
                    File file = new File(Utils.dataPath + getFileName(
                            String.valueOf(data.id), task.type));
                    File thumbCache = new File(activity.getCacheDir(), file.getName());
                    new CreateThumb(activity, DownloadListAdapter.this, data.id, type, file, thumbCache).start();
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

        void bind(DownloadItem downloadItem) {
            this.data = downloadItem;
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(downloadItem.name);
            if (downloadItem.author != null) {
                spannableStringBuilder.append("\n");
                int start = spannableStringBuilder.length();
                spannableStringBuilder.append(downloadItem.author);
                spannableStringBuilder.setSpan(new StyleSpan(Typeface.ITALIC), start, spannableStringBuilder.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableStringBuilder.setSpan(new RelativeSizeSpan(0.9f), start,
                        spannableStringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            textView.setText(spannableStringBuilder);
            DownloadRunnable task;
            if (downloadService != null && (task = downloadService.findTask(type,
                    downloadItem.id)) != null) {
                bind(task);
            } else {
                File file = new File(Utils.dataPath + getFileName(
                        String.valueOf(downloadItem.id), type));
                if (file.exists()) {
                    File thumbCache = new File(activity.getCacheDir(), file.getName());
                    if (thumbCache.exists()) {
                        Drawable drawable = Drawable.createFromPath(thumbCache.getPath());
                        progressBar.setThumb(drawable);
                    } else {
                        new CreateThumb(activity, DownloadListAdapter.this, downloadItem.id, type, file, thumbCache).start();
                    }
                    progressBar.setStatus(MyProgressBar.COMPLETED);
                    progressText.setText(DownloadRunnable.getString(file.length()));
                } else if (downloadItem.size != 0) {
                    long progress = new File(Utils.dataPath + downloadItem.id
                            + extensionOf[type]).length();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(DownloadRunnable.getString(progress)).append("/").append(DownloadRunnable.getString(downloadItem.size));
                    progressText.setText(stringBuilder);
                    progressBar.setStatus(MyProgressBar.PARTIAL_DONE);
                    progressBar.setProgress((((float) progress) / downloadItem.size) * 360);
                } else {
                    progressBar.setStatus(MyProgressBar.NONE);
                    progressText.setText(R.string.download_question);
                }
            }
        }
    }

    private void play(String path, String name) {
        activity.findViewById(R.id.loading).setVisibility(View.VISIBLE);
        Intent intent = new Intent(activity, PlayerService.class);
        activity.bindService(intent, activity, Context.BIND_ABOVE_CLIENT);
        ActivityCompat.startForegroundService(activity, intent
                .putExtra(Consts.NAME_KEY, name)
                .putExtra(Consts.MEDIA_PATH_KEY, path));
    }

    private static class DownloadItem {
        final int id;
        final String name;
        final String author;
        final String desc;
        final String url;
        long size;

        public DownloadItem(int id, String name, String author, long size, String desc, String url) {
            this.id = id;
            this.name = name;
            this.author = author;
            this.size = size;
            this.desc = desc;
            this.url = url;
        }
    }
}
