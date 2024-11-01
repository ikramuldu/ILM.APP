package bn.poro.quran.hadith_section;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import bn.poro.quran.R;

public class ResultBookListAdapter extends BaseAdapter {
    final ArrayList<HadisResult> searchResults;

    public ResultBookListAdapter(ArrayList<HadisResult> results) {
        this.searchResults = results;
    }

    @Override
    public int getCount() {
        return searchResults.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_link, parent, false);
        HadisResult result = searchResults.get(position);
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(result.bookName);
        int start = stringBuilder.length();
        stringBuilder.append("[");
        stringBuilder.append(String.valueOf(result.total));
        stringBuilder.append(" টি]");
        stringBuilder.setSpan(new ForegroundColorSpan(0xAAFF1744), start, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) convertView).setText(stringBuilder);
        return convertView;
    }
}
