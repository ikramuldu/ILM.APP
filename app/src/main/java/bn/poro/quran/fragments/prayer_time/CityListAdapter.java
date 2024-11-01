package bn.poro.quran.fragments.prayer_time;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.Consts;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.R;
import bn.poro.quran.Utils;

class CityListAdapter extends BaseAdapter implements Filterable, AdapterView.OnItemClickListener {
    private final Filter filter;
    private CityModel[] cityList;
    private final float latitude;
    private final float longitude;
    private String prefix;

    CityListAdapter(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        filter = new CityFilter();
    }

    @Override
    public int getCount() {
        return cityList == null ? 1 : cityList.length + 1;
    }

    @Override
    public String getItem(int position) {
        if (position == 0) {
            return "current location";
        } else {
            return cityList[position - 1].cityName;
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog, parent, false);
        }
        SpannableString spannableString = new SpannableString(getItem(position));
        Matcher matcher = Pattern.compile(prefix, Pattern.CASE_INSENSITIVE).matcher(spannableString);
        if (matcher.find()) spannableString.setSpan(new ForegroundColorSpan(0xCCCC0000),
                matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) convertView).setText(spannableString);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MainActivity activity = (MainActivity) Utils.getContext(view);
        if (position == 0) {
            activity.getLocation(true);
        } else {
            CityModel cityModel = cityList[position - 1];
            activity.saveLocation(cityModel.cityName, cityModel.latitude, cityModel.longitude);
        }
    }

    private class CityFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            SQLiteDatabase database = SQLiteDatabase.openDatabase(Utils.dataPath + Consts.PLACE_DB,
                    null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor cursor;
            if (prefix == null || prefix.length() == 0) {
                cursor = database.rawQuery("select name||', '||code,latitude,longitude from city " +
                                "order by abs(latitude - ?)+abs(longitude - ?) limit 100",
                        new String[]{String.valueOf(latitude), String.valueOf(longitude)});
            } else {
                cursor = database.rawQuery("select name||', '||code,latitude,longitude from city " +
                                "where name like ? limit 100",
                        new String[]{"%" + prefix.toString().trim() + "%"});
            }
            results.count = cursor.getCount();
            CityModel[] cityModels = new CityModel[results.count];
            for (int i = 0; i < cityModels.length; i++) {
                cursor.moveToPosition(i);
                cityModels[i] = new CityModel(cursor.getString(0),
                        cursor.getInt(1),
                        cursor.getInt(2));
            }
            results.values = cityModels;
            cursor.close();
            database.close();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            prefix = constraint.toString();
            cityList = (CityModel[]) results.values;
            notifyDataSetChanged();
        }
    }
}
