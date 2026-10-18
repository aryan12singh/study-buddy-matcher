package com.studybuddy.matching;

import com.studybuddy.course.Course;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.config.MatchingConfigSnapshot;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.scoring.ScoringRules;
import com.studybuddy.matching.strategy.MatchingStrategyType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

/**
 * Identifies the exact evaluation shown on a card. The ordered input stays on the server;
 * only its opaque digest leaves it. This is deliberately separate from the rounded,
 * anonymous evidence cache, which may reuse wording across different evaluations.
 */
@Component
public class MatchEvaluationFingerprint {
    private final JsonMapper json;

    public MatchEvaluationFingerprint(JsonMapper json) {
        this.json = json;
    }

    public String create(MatchingProfile searcher, MatchingProfile candidate, MatchSearchCriteria search,
            MatchingConfigSnapshot settings, MatchScore score, ScoringRules rules) {
        List<CriterionInput> criteria = Arrays.stream(MatchingCriterion.values())
                .map(criterion -> new CriterionInput(criterion, settings.weights().weightOf(criterion),
                        settings.enabledCriteria().contains(criterion), score.scoreFor(criterion), score.isKnown(criterion)))
                .toList();
        // Sort and limit change discovery order, not the evaluation of this candidate.
        MatchSearchCriteria context = new MatchSearchCriteria(search.courseId(), search.studyGoal(), search.studyMode(),
                search.groupSize(), search.minSharedHours(), search.minQuality());
        EvaluationInput input = new EvaluationInput("v1", profile(searcher), profile(candidate), context,
                settings.strategy().type(), settings.version(), settings.minScore(), criteria, rules,
                score.total(), score.quality(), score.sharedHoursPerWeek());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsString(input).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Every Java runtime provides SHA-256", impossible);
        }
    }

    private static ProfileInput profile(MatchingProfile profile) {
        Student student = profile.student();
        List<RelevantCourseDto> courses = student.getCoursesTaken().stream()
                .sorted(Comparator.comparing(Course::getId)).map(MatchEvaluationFingerprint::course).toList();
        return new ProfileInput(student.getId(), course(student.getTargetCourse()), courses,
                student.getPreferredStudyMode(), student.getStudyGoals().stream().sorted().toList(),
                student.getPreferredGroupSizeMin(), student.getPreferredGroupSizeMax(), profile.availability().slots());
    }

    private static RelevantCourseDto course(Course course) {
        return course == null ? null : new RelevantCourseDto(course.getId(), course.getCode(), course.getName());
    }

    private record ProfileInput(Long studentId, RelevantCourseDto targetCourse, List<RelevantCourseDto> courses,
            StudyMode studyMode, List<StudyGoal> studyGoals, Integer groupSizeMin, Integer groupSizeMax,
            List<TimeSlot> availability) {
    }

    private record CriterionInput(MatchingCriterion criterion, double weight, boolean enabled, double score, boolean known) {
    }

    private record EvaluationInput(String version, ProfileInput searcher, ProfileInput candidate, MatchSearchCriteria search,
            MatchingStrategyType strategy, long configurationVersion, double minimumScore, List<CriterionInput> criteria,
            ScoringRules rules, double totalScore, MatchQuality quality, double sharedHoursPerWeek) {
    }
}
