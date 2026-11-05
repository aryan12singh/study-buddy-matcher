package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Explains a match in one plain sentence: the quality label, then each enabled criterion in the
 * same words as the score bars (same course, matches, partly, differs, not shared) and the shared
 * hours. Deterministic, so the same score always reads the same; it is the Home card's text and
 * the fallback when AI wording is unavailable.
 */
@Component
public class TemplateMatchExplainer implements MatchExplainer {
    /** Shown wherever this text appears, because a high score is not a promise. */
    static final String CAVEAT = "A comparison of study preferences, not a probability of success.";

    @Override
    public String explain(MatchScore score, Set<MatchingCriterion> enabled) {
        String criteria = Arrays.stream(MatchingCriterion.values()).filter(enabled::contains)
                .map(criterion -> describe(score, criterion))
                .collect(Collectors.joining(", "));
        String quality = score.quality().name().charAt(0) + score.quality().name().substring(1).toLowerCase(Locale.ROOT);
        return quality + " match" + (criteria.isEmpty() ? "" : ": " + criteria) + ". " + CAVEAT;
    }

    private static String describe(MatchScore score, MatchingCriterion criterion) {
        if (!score.isKnown(criterion)) {
            return nameOf(criterion) + " not shared";
        }
        if (criterion == MatchingCriterion.AVAILABILITY) {
            return describeHours(score.sharedHoursPerWeek());
        }
        double value = score.scoreFor(criterion);
        if (value >= 1) {
            return criterion == MatchingCriterion.COURSE ? "same course" : nameOf(criterion) + " matches";
        }
        return nameOf(criterion) + (value > 0 ? " partly matches" : " differs");
    }

    /** Rounded to the nearest half hour, with "about" when rounding changed the number. */
    static String describeHours(double hours) {
        if (hours <= 0) {
            return "no shared free time";
        }
        double rounded = Math.round(hours * 2) / 2.0;
        if (rounded == 0) {
            return "under half an hour of shared time a week";
        }
        String number = rounded == Math.rint(rounded)
                ? String.valueOf((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
        String about = rounded == hours ? "" : "about ";
        return about + number + (rounded == 1 ? " shared hour" : " shared hours") + " a week";
    }

    /** The criterion's name as the score bars show it, in lower case to sit mid-sentence. */
    private static String nameOf(MatchingCriterion criterion) {
        return switch (criterion) {
            case COURSE -> "course";
            case AVAILABILITY -> "availability";
            case STUDY_MODE -> "study mode";
            case STUDY_GOAL -> "study goal";
            case GROUP_SIZE -> "group size";
        };
    }
}
