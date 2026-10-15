package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateMatchExplainerTest {

    private final TemplateMatchExplainer explainer = new TemplateMatchExplainer();

    @Test
    void describesEnabledCriteriaOnly() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.STUDY_GOAL, 0.75, MatchingCriterion.COURSE, 1.0), 0.75, 0.0,
                MatchQuality.STRONG, Set.of());

        assertEquals("strong match: study goals 75.0%. Shared availability: 0.0 hours/week. "
                + "Compatibility is not a success probability.", explainer.explain(score, Set.of(MatchingCriterion.STUDY_GOAL)));
    }

    @Test
    void saysAPreferenceIsNotSharedRatherThanQuotingAScore() {
        MatchScore score = new MatchScore(Map.of(MatchingCriterion.COURSE, 1.0, MatchingCriterion.AVAILABILITY, 0.0), 0.3, 0.0,
                MatchQuality.FAIR, Set.of(MatchingCriterion.AVAILABILITY));

        assertEquals("fair match: course 100.0%; availability not shared. Shared availability: 0.0 hours/week. "
                + "Compatibility is not a success probability.",
                explainer.explain(score, Set.of(MatchingCriterion.COURSE, MatchingCriterion.AVAILABILITY)));
    }
}
