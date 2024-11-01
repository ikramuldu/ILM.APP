package bn.poro.quran.fragments.homepage;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import bn.poro.quran.R;
import bn.poro.quran.media_section.MediaHomeActivity;

class ButtonMedia extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.islamhouse;
    }

    @Override
    public void onClick(View v) {
        Context context = v.getContext();
        context.startActivity(new Intent(context, MediaHomeActivity.class));
    }

    @Override
    int getIcon() {
        return R.drawable.ic_media;
    }
}
