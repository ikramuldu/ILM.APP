package bn.poro.quran.fragments.homepage;

import androidx.fragment.app.Fragment;

import bn.poro.quran.R;
import bn.poro.quran.workout_section.WorkoutFragment;

class ItemWorkout extends ButtonModel {
    @Override
    int getTitle() {
        return R.string.workout_title;
    }


    @Override
    Fragment getFragment() {
        return new WorkoutFragment();
    }

    @Override
    int getIcon() {
        return R.drawable.workout;
    }
}
