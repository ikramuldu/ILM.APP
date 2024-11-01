package bn.poro.quran.book_section;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import bn.poro.quran.R;

class SearchHistoryAdapter extends BaseAdapter implements Filterable, View.OnClickListener, DialogInterface.OnClickListener {
    final ArrayList<String> mainList;
    private ArrayList<String> showList;
    private final LayoutInflater inflater;
    private final ArrayFilter filter;
    private String string;

    SearchHistoryAdapter(LayoutInflater layoutInflater) {
        inflater = layoutInflater;
        filter = new ArrayFilter();
        mainList = new ArrayList<>();
        showList = mainList;
    }

    @Override
    public int getCount() {
        return showList.size();
    }

    @Override
    public String getItem(int position) {
        return showList.get(showList.size() - position - 1);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = inflater.inflate(R.layout.item_search, parent, false);
        String s = getItem(position);
        ((TextView) convertView.findViewById(R.id.text)).setText(s);
        View view = convertView.findViewById(R.id.icon);
        view.setTag(s);
        view.setOnClickListener(this);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void onClick(View v) {
        string = (String) v.getTag();
        new AlertDialog.Builder(v.getContext()).setMessage("তালিকা থেকে '" + string + "' মুছে ফেলতে চান?")
                .setPositiveButton(R.string.delete, this)
                .setTitle("Warning!!!")
                .setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mainList.remove(string);
        showList.remove(string);
        notifyDataSetChanged();
    }

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();
            final ArrayList<String> res;
            if (prefix == null || prefix.length() == 0) {
                res = new ArrayList<>(mainList);
            } else {
                final String prefixString = prefix.toString().toLowerCase();
                final int count = mainList.size();
                res = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    final String value = mainList.get(i);
                    if (value.startsWith(prefixString)) {
                        res.add(value);
                    } else {
                        final String[] words = value.split(" ");
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                res.add(value);
                                break;
                            }
                        }
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
            showList = (ArrayList<String>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
