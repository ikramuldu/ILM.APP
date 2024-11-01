package bn.poro.quran.fragments.setting_main;

import android.os.StatFs;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import bn.poro.quran.DownloadRunnable;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.activity_setting.SettingActivity;

public class DriveAdapter extends BaseAdapter {
    final String[] drives;
    private final int color;

    public DriveAdapter(String[] drives, int color) {
        this.drives = drives;
        this.color = color;
    }

    @Override
    public int getCount() {
        return drives.length;
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
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog, parent, false);
        else if ((int) convertView.getTag() == position) {
            return convertView;
        }
        convertView.setTag(position);
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (position == 0) stringBuilder.append("Phone Storage");
        else stringBuilder.append("External Memory");
        try {
            StatFs stat = new StatFs(drives[position]);
            long blockSize = stat.getBlockSizeLong();
            stringBuilder.append(" (Free: ");
            stringBuilder.append(DownloadRunnable.getString(stat.getAvailableBlocksLong() * blockSize));
            stringBuilder.append("/");
            stringBuilder.append(DownloadRunnable.getString(stat.getBlockCountLong() * blockSize));
        } catch (Exception e) {
            L.d(e);
        }
        stringBuilder.append(")\n");
        int start = stringBuilder.length();
        stringBuilder.append(drives[position]);
        int end = stringBuilder.length();
        stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) convertView).setText(stringBuilder);
        return convertView;
    }
}
