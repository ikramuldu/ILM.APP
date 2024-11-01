package bn.poro.quran.book_section;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import bn.poro.quran.Consts;
import bn.poro.quran.DownloadService;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class BookSearchAdapter extends BaseAdapter implements Filterable, View.OnClickListener {
    private final SparseArray<String> mainList;
    private SparseArray<String> showList;
    private final ArrayFilter filter;

    BookSearchAdapter(SparseArray<String> list) {
        this.mainList = list;
        filter = new ArrayFilter();
        showList = mainList;
    }

    @Override
    public int getCount() {
        return showList.size();
    }

    @Override
    public String getItem(int position) {
        return showList.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_search, parent, false);
            convertView.setOnClickListener(this);
        }
        String s = getItem(position);
        SpannableString spannableString = new SpannableString(s);
        spannableString.setSpan(new RelativeSizeSpan(0.8f), s.indexOf("\n") + 1, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        convertView.setTag(position);
        ((TextView) convertView).setText(spannableString);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();
        String name = showList.valueAt(position);
        name = name.substring(0, name.indexOf("\n"));
        String book = String.valueOf(showList.keyAt(position));
        Context context = v.getContext();
        if (new File(Utils.dataPath + Consts.BOOK_SUB_PATH, book).exists()) {
            context.startActivity(new Intent(context, ReadBookActivity.class).putExtra(Consts.ID_KEY, book).putExtra(Consts.TITLE_KEY, name));
        } else {
            context.startService(new Intent(context, DownloadService.class)
                    .putExtra(Consts.ID_KEY, Integer.parseInt(book))
                    .putExtra(Consts.NAME_KEY, name)
                    .putExtra(Consts.URL_KEY, DownloadService.BASE_URL + Consts.BOOK_SUB_PATH + book + ".zip")
                    .putExtra(Consts.EXTRACTION_PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + book)
                    .putExtra(Consts.PATH_KEY, Utils.dataPath + Consts.BOOK_SUB_PATH + book + ".zip")
                    .putExtra(Consts.TYPE_KEY, DownloadService.TYPE_BOOK));
            Toast.makeText(context, R.string.loading, Toast.LENGTH_SHORT).show();
        }
    }

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();
            final SparseArray<String> res;
            if (prefix == null || prefix.length() == 0) {
                res = mainList.clone();
            } else {
                final String prefixString = prefix.toString().toLowerCase();
                final int count = mainList.size();
                res = new SparseArray<>();
                for (int i = 0; i < count; i++) {
                    final String value = mainList.valueAt(i);
                    if (value.contains(prefixString)) {
                        res.append(mainList.keyAt(i), value);
                    }
                }
            }
            results.values = res;
            results.count = res.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            showList = (SparseArray<String>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
