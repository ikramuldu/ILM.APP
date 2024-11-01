package bn.poro.quran.workout_section;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.Arrays;
import java.util.Random;

import bn.poro.quran.Consts;
import bn.poro.quran.L;
import bn.poro.quran.R;

public class WorkoutActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, DialogInterface.OnClickListener {
    private static final int TOTAL_TIME = 1400 * 1000;
    private static final int FPS_INTERVAL = 67;
    int[] checkedItems;
    int workoutIndex, restTime, workTime, counterStartTime;
    WorkoutProgress progress;
    private boolean halfNotified, isResting = true;
    private TextToSpeech tts;
    private CountDownTimer timer;
    private final OnBackPressedCallback callback=new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            pause();
            new AlertDialog.Builder(WorkoutActivity.this)
                    .setTitle("Finish Workout?")
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton("Yes", WorkoutActivity.this)
                    .show();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.workout);
        progress = findViewById(R.id.progress);
        SharedPreferences store = getSharedPreferences(Consts.STORE_NAME, MODE_PRIVATE);
        progress.text = store.getInt(Consts.REST_KEY, WorkoutFragment.DEF_REST);
        restTime = progress.text * 1000;
        int[] arr = new int[WorkoutFragment.gifs.length];
        int checkedInt = getIntent().getIntExtra(Consts.CHECKED_ITEMS, 0);
        int i;
        int j = 0;
        for (i = 0; i < WorkoutFragment.gifs.length; i++) {
            if ((checkedInt & 1 << i) != 0) arr[j++] = i;
        }
        checkedItems = Arrays.copyOf(arr, j);
        if (getIntent().getIntExtra(WorkoutFragment.BUTTON_ID, 0) == R.id.randomly) {
            shuffleArray(checkedItems);
        }
        workTime = store.getInt(Consts.WORK_KEY, WorkoutFragment.DEF_WORK) * 1000;
        tts = new TextToSpeech(this, this);
        ((TextView) findViewById(R.id.text)).setText(R.string.get_ready);

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    void shuffleArray(int[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (timer == null) resume();
        else pause();
    }

    private void pause() {
        timer.cancel();
        ((ImageView) findViewById(R.id.image)).setImageResource(R.drawable.start_workout);
        timer = null;
        callback.setEnabled(false);
    }

    private void resume() {
        Glide.with(WorkoutActivity.this).load(WorkoutFragment.gifs[checkedItems[workoutIndex]]).into((ImageView) findViewById(R.id.image));
        counterStartTime = TOTAL_TIME - (restTime + workTime) * (workoutIndex + 1);
        if (isResting) counterStartTime += workTime;
        counterStartTime += progress.text * 1000;
        timer = new MyTimer().start();
        callback.setEnabled(true);
    }

    @Override
    protected void onStop() {
        if (timer != null) pause();
        super.onStop();
    }

    @Override
    public void onInit(int status) {
        if (!isFinishing())
            resume();
        if (tts != null)
            tts.speak(getString(R.string.get_ready), TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    private class MyTimer extends CountDownTimer {
        public MyTimer() {
            super(counterStartTime, FPS_INTERVAL);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int remainMS, passedMS = ((int) (TOTAL_TIME - millisUntilFinished)) - (restTime + workTime) * workoutIndex;
            if (isResting) {
                remainMS = restTime - passedMS;
                if (remainMS <= 0) {
                    isResting = false;
                    int item = checkedItems[workoutIndex];
                    final String title = getResources().getStringArray(R.array.title)[item + (item + 3) / 3];
                    if (tts != null) {
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tts.speak(title, TextToSpeech.QUEUE_FLUSH, null), 500);
                    }
                    ((TextView) findViewById(R.id.text)).setText(title);
                }
                progress.setProgress(passedMS * 360.0f / restTime);
            } else {
                passedMS -= restTime;
                remainMS = workTime - passedMS;
                if (remainMS < workTime / 2 && !halfNotified) {
                    if (tts != null)
                        tts.speak("half the time", TextToSpeech.QUEUE_FLUSH, null);
                    halfNotified = true;
                }
                if (remainMS <= 0) {
                    isResting = true;
                    workoutIndex++;
                    Handler handler = new Handler(Looper.getMainLooper());
                    if (workoutIndex == checkedItems.length) {
                        cancel();
                        if (tts != null) {
                            tts.speak("Congratulation, You have successfully completed all exercise", TextToSpeech.QUEUE_ADD, null);
                        }
                        new AlertDialog.Builder(WorkoutActivity.this)
                                .setTitle("Congratulation")
                                .setMessage("You have successfully completed all exercise.")
                                .setPositiveButton("OK", WorkoutActivity.this)
                                .setCancelable(false)
                                .show();
                    } else {
                        halfNotified = false;
                        final String takeRest = getString(R.string.take_rest);
                        int item = checkedItems[workoutIndex];
                        if (tts != null) {
                            handler.postDelayed(() -> tts.speak(takeRest, TextToSpeech.QUEUE_FLUSH, null), 500);
                            handler.postDelayed(() -> tts.speak("get ready for " + getResources().getStringArray(R.array.title)[item + (item + 3) / 3], TextToSpeech.QUEUE_ADD, null), restTime / 2);
                        }
                        ((TextView) findViewById(R.id.text)).setText(takeRest);
                        Glide.with(WorkoutActivity.this).load(WorkoutFragment.gifs[item]).into((ImageView) findViewById(R.id.image));
                    }
                }
                progress.setProgress(passedMS * 360.0f / workTime);
            }
            int rem = (remainMS + 900) / 1000;
            if (progress.text != rem) {
                progress.text = rem;
                switch (rem) {
                    case 3:
                        if (tts != null)
                            tts.speak("three", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                    case 2:
                        if (tts != null)
                            tts.speak("two", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                    case 1:
                        if (tts != null)
                            tts.speak("one", TextToSpeech.QUEUE_FLUSH, null);
                        break;
                    case 0:
                        int id;
                        if (isResting) id = R.raw.whistle;
                        else id = R.raw.ding;
                        MediaPlayer.create(WorkoutActivity.this, id).start();
                }
            }
            progress.invalidate();
        }

        @Override
        public void onFinish() {
            L.d("workout finish");
        }
    }
}
