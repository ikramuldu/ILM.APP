package bn.poro.quran.fragments.homepage;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import bn.poro.quran.BuildConfig;
import bn.poro.quran.L;
import bn.poro.quran.R;
import bn.poro.quran.Utils;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.activity_setting.SettingActivity;
import bn.poro.quran.fragments.prayer_time.PrayerTimeFragment;
import bn.poro.quran.fragments.qibla_direction.DirectionFragment;

class HomeDrawerAdapter extends RecyclerView.Adapter<HomeDrawerAdapter.Holder> {
    private static final String PAGE_LINK = "https://www.facebook.com/parobdofficial/";
    private static final String DEV_LINK = "https://play.google.com/store/apps/dev?id=7911810435620939496";
    private static final String MESSENGER_LINK = "fb-messenger://user/110109687445283";
    private final MainActivity activity;
    private final String[] items;
    private final int secondaryColor;

    HomeDrawerAdapter(MainActivity mainActivity) {
        activity = mainActivity;
        items = activity.getResources().getStringArray(R.array.home_items);
        TypedArray typedArray = activity.obtainStyledAttributes(new int[]{R.attr.tab_text});
        secondaryColor = typedArray.getColor(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            typedArray.close();
        } else
            typedArray.recycle();
    }

    private void openMessenger(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MESSENGER_LINK)));
        } catch (Exception e) {
            openFB();
        }
    }

    private void openFB() {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("fb://facewebmodal/f?href=" + PAGE_LINK)));
        } catch (Exception e) {
            openURL(PAGE_LINK);
        }
    }

    private void openURL(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(activity.getLayoutInflater().inflate(viewType == 0 ? R.layout.logo : R.layout.drawer_link, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TextView textView = holder.itemView.findViewById(R.id.text);
        switch (position) {
            case 0:
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder(activity.getString(R.string.app_name));
                stringBuilder.append("\n");
                int start = stringBuilder.length();
                stringBuilder.append("V").append(BuildConfig.VERSION_NAME);
                int end = stringBuilder.length();
                stringBuilder.setSpan(new RelativeSizeSpan(SettingActivity.SECONDARY_TEXT_SIZE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                stringBuilder.setSpan(new ForegroundColorSpan(secondaryColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(stringBuilder);
                return;
            case 3:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_share, 0, 0, 0);
                break;
            case 4:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_star, 0, 0, 0);
                break;
            case 5:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_message, 0, 0, 0);
                break;
            case 6:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_fb, 0, 0, 0);
                break;
            case 7:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_apps, 0, 0, 0);
                break;
            case 8:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_privacy, 0, 0, 0);
                break;
            case 1:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_time, 0, 0, 0);
                break;
            case 2:
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_kaaba, 0, 0, 0);
                break;
            default:
                textView.setCompoundDrawables(null, null, null, null);
        }
        textView.setText(items[position]);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return 0;
        return 1;
    }

    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        Holder(@NonNull View itemView) {
            super(itemView);
            if (itemView.getId() != R.id.more)
                itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawer_layout);
            drawerLayout.closeDrawer(GravityCompat.START);
            int position = getLayoutPosition();
            switch (position) {
                case 0:
                    break;
                case 3:
                    File file = new File(activity.getCacheDir(), activity.getString(R.string.app_name) + ".apk");
                    if (!file.exists()) try {
                        InputStream inputStream = Files.newInputStream(Paths.get(activity.getPackageManager().getApplicationInfo(BuildConfig.APPLICATION_ID, 0).publicSourceDir));
                        Utils.copy(inputStream, file);
                    } catch (Exception e) {
                        L.d(e);
                    }
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/apk");
                    intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity,
                            BuildConfig.APPLICATION_ID + ".provider", file));
                    activity.startActivity(Intent.createChooser(intent, "Share via"));
                    break;
                case 4://rating
                    activity.askReview();
                    break;
                case 5://message
                    openMessenger(activity);
                    break;
                case 6://like page
                    openFB();
                    break;
                case 7://dev page
                    openURL(DEV_LINK);
                    break;
                case 8:
                    new AlertDialog.Builder(activity)
                            .setMessage(Html.fromHtml(activity.getString(R.string.privacy_message)))
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    break;
                case 1:
                    Utils.replaceFragment(activity, new PrayerTimeFragment());
                    break;
                case 2:
                    Utils.replaceFragment(activity, new DirectionFragment());
                    break;
            }
        }
    }

}
