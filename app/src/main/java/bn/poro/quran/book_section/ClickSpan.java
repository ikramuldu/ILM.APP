package bn.poro.quran.book_section;

import android.app.AlertDialog;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

class ClickSpan extends ClickableSpan {
    final CharSequence message;

    ClickSpan(CharSequence message) {
        this.message = message;
    }

    @Override
    public void onClick(@NonNull View widget) {
        TextView textView = new AlertDialog.Builder(widget.getContext())
                .setMessage(message)
                .show().findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setColor(0xff4267B2);
        ds.setUnderlineText(false);
    }
}
