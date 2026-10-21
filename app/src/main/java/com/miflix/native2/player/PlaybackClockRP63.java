package com.miflix.native2.player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** AM/PM presentation, using the TV's timezone. Playback durations remain mm:ss. */
public final class PlaybackClockRP63 {
    private PlaybackClockRP63() {}
    public static String format(long epochMillis) {
        return new SimpleDateFormat("h:mm a",Locale.US).format(new Date(epochMillis));
    }
}
