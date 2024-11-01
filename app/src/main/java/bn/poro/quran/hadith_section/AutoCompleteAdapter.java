package bn.poro.quran.hadith_section;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import bn.poro.quran.L;
import bn.poro.quran.R;

class AutoCompleteAdapter extends BaseAdapter implements Filterable, View.OnClickListener, DialogInterface.OnClickListener {
    private final ArrayList<String> mainList;
    private ArrayList<String> showList;
    private final ArrayFilter filter;
    private String s;

    AutoCompleteAdapter(ArrayList<String> mainList) {
        this.mainList = mainList;
        filter = new ArrayFilter();
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
        String s = getItem(position);
        if (convertView == null)
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search, parent, false);
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
        s = (String) v.getTag();
        new AlertDialog.Builder(v.getContext()).setMessage("তালিকা থেকে '" + s + "' মুছে ফেলতে চান?")
                .setPositiveButton(R.string.delete, this)
                .setTitle("Warning!!!")
                .setNegativeButton(R.string.cancel, null).show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mainList.remove(s);
        showList.remove(s);
        notifyDataSetChanged();
    }

    public void refresh(String s) {
        mainList.remove(s);
        mainList.add(s);
    }

    public void saveAt(File file) {
        try {
            OutputStream outputStream = Files.newOutputStream(file.toPath());
            int len = mainList.size();
            for (int i = 0; i < len; i++) {
                outputStream.write(mainList.get(i).getBytes());
                outputStream.write('\n');
            }
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            L.d(e);
        }
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
