package com.studybuddy.matching;

import com.studybuddy.course.Course;
import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class MatchResultAssembler {
    public MatchResultDto toDto(MatchingProfile searcher, MatchingProfile candidate, MatchScore score,
            MatchSearchCriteria search, Set<MatchingCriterion> enabled, RelationshipState relationship) {
        Student student = candidate.student();
        List<TimeSlot> shared = searcher.availability().sharedSlotsWith(candidate.availability());
        return new MatchResultDto(student.getId(), student.getName(), student.getSchool(), student.getProgramme(),
                student.getYearOfStudy(), student.getPreferredStudyMode(), student.getStudyGoals().stream().sorted().toList(),
                score.quality(), score.total(), score.explanation(), relevantCourse(searcher.student(), student, search),
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
        List<Course> theirs = coursesOf(other).toList();
        Stream<Course> relevant = search.courseId() == null
                ? coursesOf(self).filter(course -> theirs.stream().anyMatch(peer -> peer.getId().equals(course.getId())))
                : theirs.stream().filter(course -> course.getId().equals(search.courseId()));
        return relevant.sorted(Comparator.comparing(Course::getCode).thenComparing(Course::getId)).findFirst()
                .map(course -> new RelevantCourseDto(course.getId(), course.getCode(), course.getName())).orElse(null);
    }

    private static Stream<Course> coursesOf(Student student) {
        return Stream.concat(Stream.ofNullable(student.getTargetCourse()), student.getCoursesTaken().stream());
    }

    private static SharedSlotDto toDto(TimeSlot slot) { return new SharedSlotDto(slot.day(), slot.start(), slot.end()); }
}
