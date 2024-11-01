package bn.poro.quran.fragments.alphabet;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

class PlayAudioOnCLick extends ClickableSpan {
    private final String audioId;
    private final AlphabetAdapter alphabetAdapter;

    PlayAudioOnCLick(AlphabetAdapter adapter, String s) {
        alphabetAdapter = adapter;
        audioId = s;
    }

    @Override
    public void onClick(@NonNull View widget) {
        alphabetAdapter.play(audioId);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
    }
}
