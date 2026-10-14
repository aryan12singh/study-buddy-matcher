package com.studybuddy.matching.scoring;

import com.studybuddy.course.Course;
import com.studybuddy.matching.MatchingFixtures;
import com.studybuddy.matching.MatchQuality;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.QualityThresholds;
import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.student.Student;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.studybuddy.matching.MatchingFixtures.course;
import static com.studybuddy.matching.MatchingFixtures.evenWeights;
import static com.studybuddy.matching.MatchingFixtures.profile;
import static com.studybuddy.matching.MatchingFixtures.searchForCourse;
import static com.studybuddy.matching.MatchingFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedMatchScorerTest {

    private static final MatchingProfile SELF = new MatchingProfile(null, new WeeklyAvailability(List.of()));
    private static final MatchingProfile CANDIDATE = new MatchingProfile(null, new WeeklyAvailability(List.of()));
    private static final MatchSearchCriteria SEARCH = new MatchSearchCriteria(1L, null, null, null, null, null);
    private static final Set<MatchingCriterion> ALL = EnumSet.allOf(MatchingCriterion.class);
    private static final QualityThresholds THRESHOLDS = new QualityThresholds(0.75, 0.50);
    private static final ScoringRules UNKNOWN_IS_ZERO = MatchingFixtures.rules();

    /** A scorer that always returns the same result, so the weighting can be checked on its own. */
    private record FixedScorer(MatchingCriterion criterion, CriterionResult result) implements CriterionScorer {
        FixedScorer(MatchingCriterion criterion, double score) {
            this(criterion, CriterionResult.of(score));
        }

        @Override
        public CriterionResult score(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search) {
            return result;
        }
    }

    private static WeightedMatchScorer scorer(List<CriterionScorer> scorers, ScoringRules rules) {
        return new WeightedMatchScorer(scorers, THRESHOLDS, rules);
    }

    private static List<CriterionScorer> allCriteriaScoring(double score) {
        return List.of(MatchingCriterion.values()).stream()
                .<CriterionScorer>map(criterion -> new FixedScorer(criterion, score))
                .toList();
    }

    @Test
    void totalIsTheWeightedSumOfTheCriterionScores() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.COURSE, 0.6);
        values.put(MatchingCriterion.STUDY_MODE, 0.1);
        values.put(MatchingCriterion.STUDY_GOAL, 0.05);
        values.put(MatchingCriterion.GROUP_SIZE, 0.05);
        WeightedMatchScorer scorer = scorer(List.of(
                new FixedScorer(MatchingCriterion.COURSE, 1.0),
                new FixedScorer(MatchingCriterion.AVAILABILITY, 0.5),
                new FixedScorer(MatchingCriterion.STUDY_MODE, 0.0),
                new FixedScorer(MatchingCriterion.STUDY_GOAL, 0.0),
                new FixedScorer(MatchingCriterion.GROUP_SIZE, 0.0)), UNKNOWN_IS_ZERO);

        MatchScore score = scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(values), ALL);

        // 0.6 * 1.0 + 0.2 * 0.5 + 0 + 0 + 0
        assertEquals(0.7, score.total(), 1e-9);
        assertEquals(1.0, score.scoreFor(MatchingCriterion.COURSE));
        assertEquals(0.5, score.scoreFor(MatchingCriterion.AVAILABILITY));
    }

    @Test
    void perfectScoresGiveATotalOfExactlyOne() {
        Map<MatchingCriterion, Double> values = evenWeights();
        values.put(MatchingCriterion.COURSE, 0.1);
        values.put(MatchingCriterion.AVAILABILITY, 0.5);
        values.put(MatchingCriterion.STUDY_GOAL, 0.1);
        values.put(MatchingCriterion.GROUP_SIZE, 0.1);
        WeightedMatchScorer scorer = scorer(allCriteriaScoring(1.0), UNKNOWN_IS_ZERO);

        assertEquals(1.0, scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(values), ALL).total(), 1e-9);
    }

    @Test
    void disabledScorersAreNotInvokedAndExplanationUsesEnabledCriteriaOnly() {
        CriterionScorer disabled = new CriterionScorer() {
            public MatchingCriterion criterion() { return MatchingCriterion.COURSE; }
            public CriterionResult score(MatchingProfile self, MatchingProfile peer, MatchSearchCriteria search) {
                throw new AssertionError("Disabled criterion was evaluated");
            }
        };
        var weights = evenWeights(); weights.replaceAll((criterion, value) -> criterion == MatchingCriterion.STUDY_GOAL ? 1.0 : 0.0);
        WeightedMatchScorer scorer = scorer(List.of(disabled, new FixedScorer(MatchingCriterion.STUDY_GOAL, 0.75)), UNKNOWN_IS_ZERO);

        MatchScore score = scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(weights), Set.of(MatchingCriterion.STUDY_GOAL));

        assertEquals(0.75, score.total());
        assertEquals(MatchQuality.STRONG, score.quality());
        assertEquals(0.0, score.scoreFor(MatchingCriterion.COURSE));
        assertEquals("strong match: study goals 75.0%. Shared availability: 0.0 hours/week. Compatibility is not a success probability.", score.explanation());
    }

    @Test
    void criterionWithNoScorerCountsAsZero() {
        WeightedMatchScorer scorer = scorer(List.of(new FixedScorer(MatchingCriterion.COURSE, 1.0)), UNKNOWN_IS_ZERO);

        MatchScore score = scorer.score(SELF, CANDIDATE, SEARCH, new MatchingWeights(evenWeights()), ALL);

        assertEquals(0.2, score.total(), 1e-9);
        assertEquals(0.0, score.scoreFor(MatchingCriterion.AVAILABILITY));
    }

    @Test
    void unknownCriteriaUseTheConfiguredUnknownScoreAndAreFlagged() {
        List<CriterionScorer> scorers = List.of(new FixedScorer(MatchingCriterion.COURSE, 1.0),
                new FixedScorer(MatchingCriterion.AVAILABILITY, CriterionResult.unknown()));

        MatchScore zero = scorer(scorers, UNKNOWN_IS_ZERO).score(SELF, CANDIDATE, SEARCH, new MatchingWeights(evenWeights()), ALL);
        MatchScore half = scorer(scorers, new ScoringRules(0.5, 0.5)).score(SELF, CANDIDATE, SEARCH, new MatchingWeights(evenWeights()), ALL);

        assertEquals(0.0, zero.scoreFor(MatchingCriterion.AVAILABILITY));
        assertEquals(0.5, half.scoreFor(MatchingCriterion.AVAILABILITY));
        assertFalse(zero.isKnown(MatchingCriterion.AVAILABILITY));
        assertTrue(zero.isKnown(MatchingCriterion.COURSE));
        assertTrue(zero.explanation().contains("availability not shared"), zero.explanation());
    }

    @Test
    void aProfileWithOnlyACourseCannotLookLikeAGoodMatch() {
        // Before unknowns scored zero, four empty preferences were worth 0.5 each and
        // a profile with nothing but a target course was labelled GOOD.
        Course is442 = course(4, "IS442");
        Student me = student(1, "Priya");
        Student emptyProfile = student(2, "Empty");
        emptyProfile.setTargetCourse(is442);
        WeightedMatchScorer scorer = scorer(List.of(new CourseScorer(MatchingFixtures.rules()), new AvailabilityOverlapScorer(), new StudyModeScorer(),
                new StudyGoalScorer(), new GroupSizeScorer()), UNKNOWN_IS_ZERO);
        MatchingWeights balanced = new MatchingWeights(Map.of(MatchingCriterion.COURSE, 0.30, MatchingCriterion.AVAILABILITY, 0.30,
                MatchingCriterion.STUDY_MODE, 0.20, MatchingCriterion.STUDY_GOAL, 0.10, MatchingCriterion.GROUP_SIZE, 0.10));

        MatchScore score = scorer.score(profile(me), profile(emptyProfile), searchForCourse(4), balanced, ALL);

        assertEquals(0.30, score.total(), 1e-9);
        assertEquals(MatchQuality.FAIR, score.quality());
        assertEquals(EnumSet.of(MatchingCriterion.AVAILABILITY, MatchingCriterion.STUDY_MODE, MatchingCriterion.STUDY_GOAL,
                MatchingCriterion.GROUP_SIZE), score.unknownCriteria());
    }
}
