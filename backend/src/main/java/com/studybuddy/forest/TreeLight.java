package com.studybuddy.forest;

import java.time.LocalTime;

/** The light a tree was planted in, from the visit's local start time. It sets the tree's colours. */
public enum TreeLight {
    DAWN, DAY, DUSK, NIGHT;

    public static TreeLight at(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 11) return DAWN;
        if (hour >= 11 && hour < 17) return DAY;
        if (hour >= 17 && hour < 21) return DUSK;
        return NIGHT;
    }
}
