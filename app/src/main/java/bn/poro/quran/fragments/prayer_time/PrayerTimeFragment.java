package bn.poro.quran.fragments.prayer_time;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

import bn.poro.quran.Consts;
import bn.poro.quran.MyLayoutManager;
import bn.poro.quran.R;
import bn.poro.quran.activity_main.MainActivity;
import bn.poro.quran.views.InstantCompleteView;

public class PrayerTimeFragment extends Fragment implements
        View.OnClickListener,
        DatePickerDialog.OnDateSetListener,
        View.OnFocusChangeListener {

    public static final long SUHR_CAUTION_TIME = 3 * MINUTE_IN_MILLIS;
    public static final long MIDDAY_CAUTION_TIME = 2 * MINUTE_IN_MILLIS;
    public static final long SUNRISE_SUNSET_TIME = 15 * MINUTE_IN_MILLIS;
    public static final long MAGRIB_CAUTION_TIME = 3 * MINUTE_IN_MILLIS;
    PrayerTimeAdapter adapter;
    private long shownDate;
    private View fragmentView;
    private PrayerTimer timer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.prayer_time, container, false);
        MainActivity activity = (MainActivity) inflater.getContext();
        RecyclerView recyclerView = fragmentView.findViewById(R.id.main_list);
        recyclerView.setLayoutManager(new MyLayoutManager(activity));
        shownDate = System.currentTimeMillis();
        Date date = new Date(shownDate);
        adapter = new PrayerTimeAdapter(activity, date);
        recyclerView.setAdapter(adapter);
        InstantCompleteView editText = fragmentView.findViewById(R.id.edit);
        SharedPreferences preferences = activity.getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE);
        activity.getLocation(!preferences.contains(Consts.LATITUDE));
        int latitude = preferences.getInt(Consts.LATITUDE, Consts.LATITUDE_DHAKA);
        int longitude = preferences.getInt(Consts.LONGITUDE, Consts.LONGITUDE_DHAKA);
        editText.setOnFocusChangeListener(this);
        CityListAdapter listAdapter = new CityListAdapter(latitude, longitude);
        editText.setAdapter(listAdapter);
        editText.setText(preferences.getString(Consts.LOCATION_NAME, Consts.DEF_LOCATION));
        editText.setOnItemClickListener(listAdapter);
        TextView textView = fragmentView.findViewById(R.id.date);
        textView.setOnClickListener(this);
        textView.setText(new SimpleDateFormat("dd/MM/yyyy", MainActivity.getLocale()).format(date));
        fragmentView.findViewById(R.id.prev).setOnClickListener(this);
        fragmentView.findViewById(R.id.next).setOnClickListener(this);
        timer = new PrayerTimer(this);
        timer.start();
        return fragmentView;
    }

    @Override
    public void onDestroyView() {
        timer.cancel();
        super.onDestroyView();
    }

    public void update() {
        adapter.refresh(new Date(shownDate));
        adapter.notifyItemRangeChanged(0, adapter.getItemCount(), PrayerTimeAdapter.UPDATE_ALL);
        EditText editText = fragmentView.findViewById(R.id.edit);
        editText.setText(requireContext().getSharedPreferences(Consts.STORE_NAME, Context.MODE_PRIVATE).getString(Consts.LOCATION_NAME, ""));
        editText.clearFocus();
    }

    @Override
    public void onClick(View v) {
        long t = shownDate;
        if (v.getId() == R.id.prev) t = t - DAY_IN_MILLIS;
        else if (v.getId() == R.id.next) t = t + DAY_IN_MILLIS;
        Date date = new Date(t);
        if (shownDate != t) {
            shownDate = t;
            update();
            ((TextView) ((View) v.getParent()).findViewById(R.id.date)).setText(new SimpleDateFormat("dd/MM/yyyy", MainActivity.getLocale()).format(date));
        } else
            new DatePickerDialog(v.getContext(), this, date.getYear() + 1900, date.getMonth(), date.getDate()).show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Date date = new Date(year - 1900, month, dayOfMonth);
        ((TextView) fragmentView.findViewById(R.id.date)).setText(
                new SimpleDateFormat("dd/MM/yyyy", MainActivity.getLocale()).format(date));
        shownDate = date.getTime();
        update();
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) ((TextView) v).setText("");
    }

    public void updateTime() {
        TextView textView = fragmentView.findViewById(R.id.text);
        Context context = adapter.activity;
        long time = (shownDate - shownDate % DAY_IN_MILLIS) + System.currentTimeMillis() % DAY_IN_MILLIS;
        if (time < adapter.suhrEndTime) time += DAY_IN_MILLIS;
        int max, progress, start, end;
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        if (time > adapter.juhrTime) {
            if (time > adapter.magribTime) {
                if (time > adapter.midnight) {
                    if (time > adapter.lastNightTime) {
                        max = (int) (adapter.suhrEndTime + DAY_IN_MILLIS - adapter.lastNightTime);
                        progress = (int) (time - adapter.lastNightTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.SUHR_POSITION]);
                        stringBuilder.append(context.getString(R.string.time_left));
                        if (adapter.activePosition != PrayerTimeAdapter.SUHR_POSITION ||
                                adapter.glowBorder) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.SUHR_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    } else {
                        max = (int) (adapter.lastNightTime - adapter.midnight);
                        progress = (int) (time - adapter.midnight);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.ISHA_POSITION]);
                        stringBuilder.append(context.getString(R.string.isha_makruh));
                        if (adapter.activePosition != PrayerTimeAdapter.ISHA_POSITION ||
                                !adapter.glowBorder) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.ISHA_POSITION;
                            adapter.glowBorder = true;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                    }
                } else {
                    if (time > adapter.ishaTime) {
                        max = (int) (adapter.midnight - adapter.ishaTime);
                        progress = (int) (time - adapter.ishaTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.ISHA_POSITION]);

                        if (adapter.activePosition != PrayerTimeAdapter.ISHA_POSITION ||
                                adapter.glowBorder) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.ISHA_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    } else {
                        max = (int) (adapter.ishaTime - adapter.magribTime);
                        progress = (int) (time - adapter.magribTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.MAGRIB_POSITION]);

                        if (adapter.activePosition != PrayerTimeAdapter.MAGRIB_POSITION ||
                                adapter.glowBorder) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.MAGRIB_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    }
                    stringBuilder.append(context.getString(R.string.time_left));
                }
            } else {
                if (time > adapter.onlyAsrTime) {
                    if (time > adapter.sunsetTime) {
                        max = (int) MAGRIB_CAUTION_TIME;
                        progress = (int) (time - adapter.sunsetTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.MAGRIB_POSITION]);
                        stringBuilder.append(context.getString(R.string.iftar_caution));
                        if (adapter.activePosition != PrayerTimeAdapter.MAGRIB_POSITION ||
                                !adapter.glowBorder) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.MAGRIB_POSITION;
                            adapter.glowBorder = true;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                    } else {
                        max = (int) (adapter.sunsetTime - adapter.asrHanafi);
                        progress = (int) (time - adapter.asrHanafi);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.SUNSET_POSITION]);
                        stringBuilder.append(context.getString(R.string.only_asr));
                        if (adapter.activePosition != PrayerTimeAdapter.SUNSET_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.SUNSET_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    }
                } else {
                    if (time > adapter.asrHanafi) {
                        max = (int) (adapter.sunsetTime - adapter.asrHanafi);
                        progress = (int) (time - adapter.asrHanafi);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.ASR_POSITION]);

                        if (adapter.activePosition != PrayerTimeAdapter.ASR_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.ASR_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    } else {
                        max = (int) (adapter.asrHanafi - adapter.juhrTime);
                        progress = (int) (time - adapter.juhrTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.JUHR_POSITION]);

                        if (adapter.activePosition != PrayerTimeAdapter.JUHR_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.JUHR_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    }
                    stringBuilder.append(context.getString(R.string.time_left));
                }
            }
            start = stringBuilder.length();
            stringBuilder.append(formatTime(max - progress));
            end = stringBuilder.length();
        } else {
            if (time > adapter.fazrTime) {
                if (time > adapter.ishrakTime) {
                    if (time > adapter.jawalTime) {
                        max = (int) (adapter.juhrTime - adapter.jawalTime);
                        progress = (int) (time - adapter.jawalTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.ZAWAAL_POSITION]);
                        stringBuilder.append(context.getString(R.string.haram_time));
                        start = stringBuilder.length();
                        stringBuilder.append(formatTime(max - progress));
                        end = stringBuilder.length();
                        stringBuilder.setSpan(new ForegroundColorSpan(adapter.colors[0]),
                                0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        if (adapter.activePosition != PrayerTimeAdapter.ZAWAAL_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.ZAWAAL_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    } else {
                        max = (int) (adapter.jawalTime - adapter.ishrakTime);
                        progress = (int) (time - adapter.ishrakTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.ISHRAK_POSITION]);
                        stringBuilder.append(context.getString(R.string.time_left));
                        start = stringBuilder.length();
                        stringBuilder.append(formatTime(max - progress));
                        end = stringBuilder.length();

                        if (adapter.activePosition != PrayerTimeAdapter.ISHRAK_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.ISHRAK_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    }
                } else {
                    if (time > adapter.sunriseTime) {
                        max = (int) (SUNRISE_SUNSET_TIME + MINUTE_IN_MILLIS);
                        progress = (int) (time - adapter.sunriseTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.SUNRISE_POSITION]);
                        stringBuilder.append(context.getString(R.string.haram_time));
                        start = stringBuilder.length();
                        stringBuilder.append(formatTime(max - progress));
                        end = stringBuilder.length();
                        stringBuilder.setSpan(new ForegroundColorSpan(adapter.colors[0]),
                                0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        if (adapter.activePosition != PrayerTimeAdapter.SUNRISE_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.SUNRISE_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    } else {
                        max = (int) (adapter.sunriseTime - adapter.fazrTime);
                        progress = (int) (time - adapter.fazrTime);
                        stringBuilder.append(adapter.names[PrayerTimeAdapter.FAJR_POSITION]);
                        stringBuilder.append(context.getString(R.string.time_left));
                        start = stringBuilder.length();
                        stringBuilder.append(formatTime(max - progress));
                        end = stringBuilder.length();
                        if (adapter.activePosition != PrayerTimeAdapter.FAJR_POSITION) {
                            if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                                adapter.notifyItemChanged(adapter.activePosition,
                                        PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                            }
                            adapter.activePosition = PrayerTimeAdapter.FAJR_POSITION;
                            adapter.glowBorder = false;
                            adapter.notifyItemChanged(adapter.activePosition,
                                    PrayerTimeAdapter.ADD_BORDER);
                        }
                        adapter.notifyItemChanged(adapter.activePosition,
                                (int) ((progress * PrayerTimeAdapter.MAX_LEVEL) / max));
                    }
                }
            } else {
                max = (int) SUHR_CAUTION_TIME;
                progress = (int) (time - adapter.suhrEndTime);
                stringBuilder.append(context.getString(R.string.suhr_caution));
                start = stringBuilder.length();
                stringBuilder.append(formatTime(max - progress));
                end = stringBuilder.length();
                if (adapter.activePosition != PrayerTimeAdapter.SUHR_POSITION ||
                        !adapter.glowBorder) {
                    if (adapter.activePosition != PrayerTimeAdapter.NO_POSITION) {
                        adapter.notifyItemChanged(adapter.activePosition,
                                PrayerTimeAdapter.REMOVE_HIGHLIGHT);
                    }
                    adapter.activePosition = PrayerTimeAdapter.SUHR_POSITION;
                    adapter.glowBorder = true;
                    adapter.notifyItemChanged(adapter.activePosition,
                            PrayerTimeAdapter.ADD_BORDER);
                }
            }
        }
        stringBuilder.setSpan(new RelativeSizeSpan(2f),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stringBuilder.setSpan(new StyleSpan(Typeface.BOLD),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(stringBuilder);
    }

    String formatTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", MainActivity.getLocale());
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return sdf.format(time);
    }
}

