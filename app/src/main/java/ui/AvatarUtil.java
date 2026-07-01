package ui;

import com.noamsl.fantasypt.R;

/** Picks one of the two generic vector avatars shared by every card of that gender. */
final class AvatarUtil {

    private AvatarUtil() {}

    static int resFor(String gender) {
        return "girl".equals(gender) ? R.drawable.avatar_girl : R.drawable.avatar_boy;
    }
}
