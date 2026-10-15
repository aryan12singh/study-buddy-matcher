package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Explains a match with a fixed template: the label, each enabled criterion and the shared hours. */
@Component
public class TemplateMatchExplainer implements MatchExplainer {

    @Override
    public String explain(MatchScore score, Set<MatchingCriterion> enabled) {
        String criteria = Arrays.stream(MatchingCriterion.values()).filter(enabled::contains)
                .map(criterion -> criterion.label() + " " + describe(score, criterion))
                .collect(Collectors.joining("; "));
        return score.quality().name().toLowerCase(Locale.ROOT) + " match: " + criteria
                + String.format(Locale.ROOT, ". Shared availability: %.1f hours/week. ", score.sharedHoursPerWeek())
                + "Compatibility is not a success probability.";
    }

    private static String describe(MatchScore score, MatchingCriterion criterion) {
        if (!score.isKnown(criterion)) {
            return "not shared";
        }
        return String.format(Locale.ROOT, "%.1f%%", score.scoreFor(criterion) * 100);
    }
}
