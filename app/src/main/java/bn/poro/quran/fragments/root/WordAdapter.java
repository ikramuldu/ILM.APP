package bn.poro.quran.fragments.root;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.views.FontSpan;
import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_quran.WordClickHandler;

class WordAdapter extends RecyclerView.Adapter<WordAdapter.MyHolder> {
    private final Activity activity;
    private final String[][] groups;
    private final int banglaFontSize;
    private Typeface arabicFont;
    private ChildModel[] children;
    public int expandedGroup = -2;
    private int childCount;
    private final WordClickHandler clickHandler;
    private final TextView headerText;
    private String note;

    WordAdapter(Activity activity, StickyHeader stickyHeader) {
        this.activity = activity;
        headerText = stickyHeader.headerView.findViewById(R.id.text);
        SharedPreferences store = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        banglaFontSize = store.getInt(Consts.FONT_KEY, Consts.DEF_FONT);
        int fontId = store.getInt(Consts.ARABIC_FONT_FACE, 1);
        if (fontId > 0)
            arabicFont = ResourcesCompat.getFont(activity, Consts.FONT_LIST[fontId]);
        headerText.setTextSize(banglaFontSize);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB,
                null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        Cursor cursor = database.rawQuery("select * from words", null);
        groups = new String[cursor.getCount()][4];
        for (int i = 0; i < groups.length; i++) {
            cursor.moveToPosition(i);
            for (int j = 0; j < 4; j++) {
                groups[i][j] = cursor.getString(j);
            }
        }
        cursor.close();
        database.close();
        clickHandler = new WordClickHandler(activity, arabicFont);
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case Consts.TYPE_GROUP:
                layout = R.layout.word_group;
                break;
            case Consts.TYPE_CHILD:
                layout = R.layout.word_occurance;
                break;
            default:
                layout = R.layout.textview;
        }
        return new MyHolder(LayoutInflater.from(activity).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        if (holder.itemView.getId() == R.id.group) {
            if (position == expandedGroup) holder.icon.setRotation(90);
            else holder.icon.setRotation(0);
            if (position == 0) holder.textView.setText(R.string.optional);
            else {
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
                String[] group;
                if (position <= expandedGroup) group = groups[position - 1];
                else group = groups[position - childCount - 1];
                stringBuilder.append(group[0]);
                if (arabicFont != null)
                    stringBuilder.setSpan(new FontSpan(arabicFont), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.append(" - ");
                stringBuilder.append(group[1]);
                stringBuilder.append("; ");
                stringBuilder.append(group[2]);
                holder.textView.setText(stringBuilder);
            }
        } else if (holder.itemView.getId() == R.id.word) {
            ChildModel childModel = children[position - (expandedGroup + 2)];
            // holder.itemView.setBackgroundColor(colors[(position + expandedGroup + 1) & 1]);
            SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
            stringBuilder.append(childModel.arabic);
            if (arabicFont != null)
                stringBuilder.setSpan(new FontSpan(arabicFont), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stringBuilder.append(" - ");
            stringBuilder.append(childModel.bangla);
            holder.textView.setText(stringBuilder);
            holder.textView.setTag(childModel.word);
            stringBuilder.clear();
            int sura = childModel.sura + 1;
            if (sura <= 9) stringBuilder.append("0");
            stringBuilder.append(String.valueOf(sura));
            stringBuilder.append(":");
            stringBuilder.append(String.valueOf(childModel.ayah));
            stringBuilder.append("      ".substring(stringBuilder.length()));
            ((TextView) holder.icon).setText(stringBuilder);
        } else holder.textView.setText(note);

    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position);
        else for (Object obj : payloads) {
            holder.icon.setRotation(0);
            holder.icon.setAnimation(AnimationUtils.loadAnimation(activity, (Integer) obj));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == expandedGroup + 1) return 2;
        if (position <= expandedGroup || position > expandedGroup + childCount)
            return Consts.TYPE_GROUP;
        return Consts.TYPE_CHILD;
    }

    @Override
    public int getItemCount() {
        return groups.length + 1 + childCount;
    }

    class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textView;
        final View icon;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            icon = itemView.findViewById(R.id.icon);
            if (itemView.getId() == R.id.group) {
                itemView.setOnClickListener(this);
                textView.setTextSize(banglaFontSize);
            } else if (itemView.getId() == R.id.word) {
                icon.setOnClickListener(this);
                textView.setOnClickListener(clickHandler);
                textView.setOnLongClickListener(clickHandler);
                textView.setTextSize(banglaFontSize);
            } else
                textView.setTextSize(banglaFontSize);
        }


        @Override
        public void onClick(View view) {
            int position = getLayoutPosition();
            if (view.getId() == R.id.group) {
                expand(position, textView.getText());
            } else {
                ChildModel child = children[position - expandedGroup - 2];
                activity.startActivity(new Intent(activity, QuranActivity.class)
                        .putExtra(Consts.EXTRA_SURA_ID, child.sura)
                        .putExtra(Consts.EXTRA_AYAH_NUM, child.ayah)
                        .putExtra(Consts.Extra_Word_ID, child.word));
            }
        }

    }

    public void expand(int position, CharSequence charSequence) {
        if (expandedGroup != -2) {
            notifyItemChanged(expandedGroup, R.anim.rotate_back);
            notifyItemRangeRemoved(expandedGroup + 1, childCount);
            if (position > expandedGroup) position -= childCount;
        }

        if (position == expandedGroup) {
            expandedGroup = -2;
            childCount = 0;
        } else {
            notifyItemChanged(position, R.anim.rotate);
            headerText.setText(charSequence);
            if (position == 0) {
                childCount = 1;
                note = activity.getString(R.string.about_root);
            } else {
                SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.ARABIC_DB, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                String expandType = groups[position - 1][0];
                note = groups[position - 1][3];
                File file = new File(Utils.dataPath + "5000.db");
                if (!file.exists()) try {
                    Utils.copy(activity.getAssets().open(file.getName()), file);
                } catch (Exception e) {
                    L.d(e);
                }
                database.execSQL("ATTACH DATABASE ? AS bn", new String[]{file.getPath()});
                database.execSQL("ATTACH DATABASE ? AS q", new String[]{Utils.dataPath + Consts.QURAN_DB_NAME});
                Cursor cursor = database.rawQuery("select word,arr,bn.content.text,rt,sura,ayah from (select word,group_concat(ar,'') as arr,group_concat(root,';') as rt from corpus where word in (select word from corpus where root=?) group by word) inner join bn.content on bn.content.rowid=word inner join (select q.audio.rowid as wordId,sura,ayah from q.audio left outer join q.quran on q.audio.id=q.quran.rowid) as ss on word=ss.wordId group by arr order by count(arr) desc", new String[]{expandType});
                childCount = cursor.getCount() + 1;
                children = new ChildModel[cursor.getCount()];
                for (int i = 0; i < children.length; i++) {
                    cursor.moveToPosition(i);
                    children[i] = new ChildModel(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getInt(4), cursor.getInt(5));
                }
                cursor.close();
                database.close();
            }
            notifyItemRangeInserted(position + 1, childCount);
            final int p = position;
            new Handler(Looper.getMainLooper()).postDelayed(() -> scrollTo(p, childCount), 300);
            expandedGroup = position;
        }
    }

    private void scrollTo(int start, int child) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) ((RecyclerView) activity.findViewById(R.id.main_list)).getLayoutManager();
        if (child > 5) child = 5;
        if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() - start < child)
            layoutManager.scrollToPositionWithOffset(start, 0);
    }

    private static class ChildModel {
        //word,arr,bn.content.text,rt,sura,ayah
        final int word;
        final int sura;
        final int ayah;
        final String arabic;
        final String bangla;
        final String root;

        private ChildModel(int word, String arabic, String bangla, String root, int sura, int ayah) {
            this.word = word;
            this.sura = sura;
            this.ayah = ayah;
            this.arabic = arabic;
            this.bangla = bangla;
            this.root = root;
        }
    }
}
