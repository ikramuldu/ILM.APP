package bn.poro.quran.app_updater;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import bn.poro.quran.R;

public class AppsAdapter extends BaseAdapter implements DialogInterface.OnClickListener {
    private final ArrayList<OpenableApp> apps;

    public AppsAdapter(ArrayList<OpenableApp> apps) {
        this.apps = apps;
    }

    @Override
    public int getCount() {
        return apps.size();
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
        Context context = parent.getContext();
        if (convertView == null)
            convertView = LayoutInflater.from(context).inflate(R.layout.drawer_link, parent, false);
        TextView textView = (TextView) convertView;
        OpenableApp app = apps.get(position);
        textView.setText(app.name(context));
        textView.setCompoundDrawables(app.icon(context), null, null, null);
        return textView;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        apps.get(which).open(((AlertDialog) dialog).getContext());
    }
}
