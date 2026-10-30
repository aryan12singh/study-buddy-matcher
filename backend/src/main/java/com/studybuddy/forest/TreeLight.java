package com.studybuddy.forest;

import java.time.LocalTime;

/** The light a tree was planted in, from the visit's local start time. It sets the tree's colours. */
public enum TreeLight {
    DAWN, DAY, DUSK, NIGHT;

    public static TreeLight at(LocalTime time, ForestProperties.LightHours starts) {
        int hour = time.getHour();
        if (hour >= starts.dawn() && hour < starts.day()) return DAWN;
        if (hour >= starts.day() && hour < starts.dusk()) return DAY;
        if (hour >= starts.dusk() && hour < starts.night()) return DUSK;
        return NIGHT;
    }
}
