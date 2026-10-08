package com.studybuddy.student;

/**
 * The group size a student prefers, as the brief words it: one-to-one, small
 * group, or either. {@link Student} stores it as a minimum and maximum number of
 * people so the matching engine can compare ranges the way Appendix A does
 * ("2-3 students" against "2 students" is compatible).
 */
public enum GroupSizePreference {
    ONE_TO_ONE,
    SMALL_GROUP,
    EITHER;

    /** A study pair: the smallest group a student can be matched into. */
    private static final int PAIR = 2;

    public int minimumSize() {
        return this == SMALL_GROUP ? PAIR + 1 : PAIR;
    }

    public int maximumSize(int largestGroup) {
        return this == ONE_TO_ONE ? PAIR : largestGroup;
    }

    /** Reads a stored range back as the closest of the three choices. */
    public static GroupSizePreference fromRange(Integer minimum, Integer maximum) {
        if (maximum != null && maximum <= PAIR) {
            return ONE_TO_ONE;
        }
        if (minimum != null && minimum > PAIR) {
            return SMALL_GROUP;
        }
        return EITHER;
    }
}
