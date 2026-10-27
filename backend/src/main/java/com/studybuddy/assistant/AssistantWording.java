package com.studybuddy.assistant;

import java.util.Locale;

/** Small pieces of reply wording shared by the intents. */
final class AssistantWording {
    private AssistantWording() { }

    /** SMALL_GROUP reads as "Small group". */
    static String label(Enum<?> value) {
        String words = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** 1 match, 2 matches. */
    static String count(long number, String singular, String plural) {
        return number + " " + (number == 1 ? singular : plural);
    }

    /** 120 reads as "2 hours", 90 as "1 hour 30 minutes". */
    static String duration(int minutes) {
        int hours = minutes / 60;
        int rest = minutes % 60;
        String hourPart = hours == 0 ? "" : count(hours, "hour", "hours");
        String minutePart = rest == 0 ? "" : count(rest, "minute", "minutes");
        return (hourPart + " " + minutePart).strip();
    }
}
