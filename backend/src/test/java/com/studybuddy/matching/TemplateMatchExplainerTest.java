package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The standard explanation's wording: score-bar verdicts, shared hours, unknown and disabled criteria. */
class TemplateMatchExplainerTest {
    private static final String CAVEAT = " A comparison of study preferences, not a probability of success.";

    private final TemplateMatchExplainer explainer = new TemplateMatchExplainer();

    @Test
    void readsAsOneSentenceInTheScoreBarsWords() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 1.0, MatchingCriterion.AVAILABILITY, 0.5,
                MatchingCriterion.STUDY_MODE, 1.0, MatchingCriterion.STUDY_GOAL, 0.5, MatchingCriterion.GROUP_SIZE, 0.0),
                0.8, 4.0, MatchQuality.STRONG, Set.of());

        assertEquals("Strong match: same course, 4 shared hours a week, study mode matches, study goal partly matches, "
                + "group size differs." + CAVEAT, explainer.explain(score, EnumSet.allOf(MatchingCriterion.class)));
    }

    @Test
    void describesEnabledCriteriaOnly() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.STUDY_GOAL, 0.75, MatchingCriterion.COURSE, 1.0), 0.75, 0.0,
                MatchQuality.STRONG, Set.of());

        assertEquals("Strong match: study goal partly matches." + CAVEAT, explainer.explain(score, Set.of(MatchingCriterion.STUDY_GOAL)));
    }

    @Test
    void saysAPreferenceIsNotSharedRatherThanQuotingAScore() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 0.5, MatchingCriterion.AVAILABILITY, 0.0), 0.3, 0.0,
                MatchQuality.FAIR, Set.of(MatchingCriterion.AVAILABILITY));

        assertEquals("Fair match: course partly matches, availability not shared." + CAVEAT,
                explainer.explain(score, Set.of(MatchingCriterion.COURSE, MatchingCriterion.AVAILABILITY)));
    }

    @Test
    void saysWhenThereIsNoSharedFreeTime() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.AVAILABILITY, 0.0), 0.4, 0.0, MatchQuality.GOOD, Set.of());

        assertEquals("Good match: no shared free time." + CAVEAT, explainer.explain(score, Set.of(MatchingCriterion.AVAILABILITY)));
    }

    @Test
    void withNoEnabledCriteriaGivesOnlyTheLabel() {
        MatchScore score = new MatchScore(Map.of(), 0.0, 0.0, MatchQuality.FAIR, Set.of());

        assertEquals("Fair match." + CAVEAT, explainer.explain(score, Set.of()));
    }

    @Test
    void roundsSharedHoursToTheNearestHalfHour() {
        assertEquals("1 shared hour a week", TemplateMatchExplainer.describeHours(1.0));
        assertEquals("1.5 shared hours a week", TemplateMatchExplainer.describeHours(1.5));
        assertEquals("about 1.5 shared hours a week", TemplateMatchExplainer.describeHours(1.4));
        assertEquals("about 3 shared hours a week", TemplateMatchExplainer.describeHours(2.9));
        assertEquals("under half an hour of shared time a week", TemplateMatchExplainer.describeHours(0.2));
        assertEquals("no shared free time", TemplateMatchExplainer.describeHours(0.0));
    }
}
