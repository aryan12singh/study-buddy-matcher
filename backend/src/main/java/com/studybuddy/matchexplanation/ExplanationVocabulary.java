package com.studybuddy.matchexplanation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Words the AI provider sees in place of code names. A model copies whatever it is given, so
 * sending {@code EXAM_PREPARATION} produces "EXAM_PREPARATION" in the student's explanation;
 * sending "exam preparation" produces readable text. Only the prompt uses this view: the
 * evidence record, and therefore the cache key, keep their typed values.
 */
public final class ExplanationVocabulary {

    private ExplanationVocabulary() {
    }

    /**
     * The evidence with the same fields, in the same order, but every enum written as plain words.
     * The matching strategy is left out: it is admin vocabulary that means nothing to a student.
     */
    public static Map<String, Object> describe(ExplanationEvidence evidence) {
        Map<String, Object> words = new LinkedHashMap<>();
        words.put("quality", words(evidence.quality()));
        words.put("totalScore", evidence.totalScore());
        words.put("criteria", evidence.criteria().stream().map(ExplanationVocabulary::describe).toList());
        words.put("sharedHoursPerWeek", evidence.sharedHoursPerWeek());
        words.put("scoringCourse", evidence.scoringCourse());
        words.put("sharedCourse", evidence.sharedCourse());
        words.put("searchedGoal", words(evidence.searchedGoal()));
        words.put("sharedGoals", evidence.sharedGoals().stream().map(ExplanationVocabulary::words).toList());
        words.put("searcherStudyMode", words(evidence.searcherStudyMode()));
        words.put("candidateStudyMode", words(evidence.candidateStudyMode()));
        words.put("searcherGroupSize", words(evidence.searcherGroupSize()));
        words.put("candidateGroupSize", words(evidence.candidateGroupSize()));
        return words;
    }

    private static Map<String, Object> describe(CriterionEvidence criterion) {
        Map<String, Object> words = new LinkedHashMap<>();
        words.put("criterion", criterion.criterion().label());
        words.put("score", criterion.score());
        words.put("weight", criterion.weight());
        words.put("enabled", criterion.enabled());
        words.put("known", criterion.known());
        return words;
    }

    /** {@code ONE_TO_ONE} becomes "one to one"; a missing value stays missing. */
    private static String words(Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
