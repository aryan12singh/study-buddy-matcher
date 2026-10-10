package com.studybuddy.matching;

/** The plain-language label students see instead of a raw score. Declared best first. */
public enum MatchQuality {
    STRONG,
    GOOD,
    FAIR;

    public boolean isAtLeast(MatchQuality minimum) {
        return ordinal() <= minimum.ordinal();
    }
}
