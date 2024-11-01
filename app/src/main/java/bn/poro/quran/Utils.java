package bn.poro.quran;

import static java.nio.file.StandardOpenOption.READ;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.transition.Slide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bn.poro.quran.activity_quran.QuranActivity;
import bn.poro.quran.activity_quran.ScrollSpan;
import bn.poro.quran.views.FontSpan;

public class Utils {

    public static final int BUFFER_SIZE = 8 * 1024;
    public static String dataPath;

    @NonNull
    public static CharSequence formatNum(int num) {
        return String.format("%d", num);
    }

    public static Context getContext(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context;
    }

    public static void move(File from, File to) {
        if (from.renameTo(to)) return;
        if (copy(from, to)) {
            from.delete();
        }
    }

    public static boolean copy(File from, File to) {
        try (InputStream inputStream = Files.newInputStream(from.toPath())) {
            copy(inputStream, to);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void copy(InputStream inputStream, File to) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(to.toPath())) {
            copy(inputStream, outputStream);
        }
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        int len;
        byte[] buffer = new byte[Utils.BUFFER_SIZE];
        while ((len = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, len);
        outputStream.flush();
    }

    public static void deleteFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File file : files) {
            if (file.isDirectory()) deleteFiles(file);
            file.delete();
        }
    }

    public static void replaceFragment(FragmentActivity activity, Fragment fragment) {
        fragment.setEnterTransition(new Slide(Gravity.END));
        fragment.setExitTransition(new Slide(Gravity.START));
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public static boolean notContains(String[] strings, String search) {
        for (String s : strings) if (s.equals(search)) return false;
        return true;
    }

    public static void copyFromAssets(Activity activity, String name) throws IOException {
        File file = new File(Utils.dataPath + name);
        deleteDB(file);
        Utils.copy(activity.getAssets().open(name), file);
    }

    public static void deleteDB(File file) {
        file.delete();
        new File(file.getPath() + "-wal").delete();
        new File(file.getPath() + "-shm").delete();
        new File(file.getPath() + "-journal").delete();
    }

    public static int parseNumber(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= '০')
                chars[i] = (char) ('0' + chars[i] - '০');
        }
        return Integer.parseInt(new String(chars));
    }

    public static SpannableString processTranslation(QuranActivity activity, String languageName, CharSequence translation) {
        String lineBreak;
        String localTranslation;
        if (Utils.notContains(Consts.RTL, languageName.substring(0, languageName.indexOf('-')))) {
            localTranslation = "\u200E" + translation;
            lineBreak = "<br>\u200E";
        } else {
            lineBreak = "<br>";
            localTranslation = translation.toString();
        }

        SpannableString spannableString = new SpannableString(Html.fromHtml(localTranslation.replaceAll("\n", lineBreak)
                .replaceAll("ہ", "ه")
                .replaceAll("ی", "ي")
                .replaceAll("ک", "ك")
                .replaceAll("ۃ", "ة")));
        if (activity.arabicFont != null) {
            Matcher matcher = Pattern.compile(Consts.ARABIC_MATCHER).matcher(spannableString);
            while (matcher.find()) {
                spannableString.setSpan(new FontSpan(activity.arabicFont), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        Matcher matcher = Pattern.compile("([১২৩৪৫৬৭৮৯০\\d]+)[ঃ:]([১২৩৪৫৬৭৮৯০\\d]+)").matcher(spannableString);
        while (matcher.find()) {
            int sura = Utils.parseNumber(matcher.group(1));
            int ayah = Utils.parseNumber(matcher.group(2));
            spannableString.setSpan(new ScrollSpan(activity, sura, ayah), matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    public static ArrayList<Integer> readAlarmData(Context context) {
        ArrayList<Integer> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), Consts.ALARM_DATA_FILE);
        if (file.exists())
            try (InputStream inputStream = Files.newInputStream(file.toPath(),READ)) {
                int b;
                while ((b = inputStream.read()) != -1) {
                    list.add(b);
                }
            } catch (Exception e) {
                L.d(e);
            }
        return list;
    }

    public static void saveAlarmData(Context context, ArrayList<Integer> list) {
        File file = new File(context.getFilesDir(), Consts.ALARM_DATA_FILE);
        try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
            for (Integer b : list) {
                outputStream.write(b);
            }
            outputStream.flush();
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            L.d(e);
        }
    }

    public static void copy(File file, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            copy(inputStream, outputStream);
        }
    }

    public static String getLanguageName(String lang) {
        Locale locale = new Locale(lang);
        String localeName = locale.getDisplayName(locale);
        String localeName2 = locale.getDisplayName();
        return localeName.equals(localeName2) ? localeName : localeName + " - " + localeName2;
    }

}
