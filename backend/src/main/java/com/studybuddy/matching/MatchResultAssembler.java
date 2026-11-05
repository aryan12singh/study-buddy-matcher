package com.studybuddy.matching;

import com.studybuddy.course.Course;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.config.MatchingConfigSnapshot;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.matching.scoring.ScoringRules;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Builds one {@link MatchResultDto} from a scored candidate: the public profile fields, the score
 * and its per-criterion breakdown, the explanation, the shared weekly slots and the course that
 * makes the match relevant. It never includes the candidate's contact number.
 */
@Component
public class MatchResultAssembler {

    private final ScoringRules rules;
    private final MatchExplainer explainer;
    private final MatchEvaluationFingerprint fingerprints;

    public MatchResultAssembler(ScoringRules rules, MatchExplainer explainer, MatchEvaluationFingerprint fingerprints) {
        this.rules = rules;
        this.explainer = explainer;
        this.fingerprints = fingerprints;
    }

    public MatchResultDto toDto(MatchingProfile searcher, MatchingProfile candidate, MatchScore score,
            MatchSearchCriteria search, MatchingConfigSnapshot settings, RelationshipState relationship) {
        Student student = candidate.student();
        Set<MatchingCriterion> enabled = settings.enabledCriteria();
        List<TimeSlot> shared = searcher.availability().sharedSessionsWith(candidate.availability(), rules.minSessionMinutes());
        return new MatchResultDto(student.getId(), student.getName(), student.getSchool(), student.getProgramme(),
                student.getYearOfStudy(), student.getPreferredStudyMode(), student.getStudyGoals().stream().sorted().toList(),
                score.quality(), score.total(), explainer.explain(score, enabled),
                fingerprints.create(searcher, candidate, search, settings, score, rules), relevantCourse(searcher.student(), student, search),
                breakdownOf(score, enabled), score.sharedHoursPerWeek(), shared.stream().map(MatchResultAssembler::toDto).toList(),
                relationship);
    }

    private static List<CriterionScoreDto> breakdownOf(MatchScore score, Set<MatchingCriterion> enabled) {
        return Arrays.stream(MatchingCriterion.values())
                .map(criterion -> new CriterionScoreDto(criterion, score.scoreFor(criterion), enabled.contains(criterion),
                        score.isKnown(criterion)))
                .toList();
    }

    private static RelevantCourseDto relevantCourse(Student self, Student other, MatchSearchCriteria search) {
        if (search.courseId() == null) {
            return sharedCourse(self, other);
        }
        return coursesOf(other).filter(course -> course.getId().equals(search.courseId()))
                .findFirst().map(MatchResultAssembler::courseDto).orElse(null);
    }

    static RelevantCourseDto sharedCourse(Student self, Student other) {
        List<Course> theirs = coursesOf(other).toList();
        return coursesOf(self).filter(course -> theirs.stream().anyMatch(peer -> peer.getId().equals(course.getId())))
                .sorted(Comparator.comparing(Course::getCode).thenComparing(Course::getId)).findFirst()
                .map(MatchResultAssembler::courseDto).orElse(null);
    }

    static RelevantCourseDto courseDto(Course course) {
        return course == null ? null : new RelevantCourseDto(course.getId(), course.getCode(), course.getName());
    }

    private static Stream<Course> coursesOf(Student student) {
        return Stream.concat(Stream.ofNullable(student.getTargetCourse()), student.getCoursesTaken().stream());
    }

    private static SharedSlotDto toDto(TimeSlot slot) { return new SharedSlotDto(slot.day(), slot.start(), slot.end()); }
}
