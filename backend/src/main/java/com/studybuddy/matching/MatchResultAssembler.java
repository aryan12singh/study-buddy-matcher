package com.studybuddy.matching;

import com.studybuddy.matching.availability.TimeSlot;
import com.studybuddy.matching.scoring.MatchScore;
import com.studybuddy.matching.scoring.MatchingCriterion;
import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class MatchResultAssembler {

    private static final double MINUTES_PER_HOUR = 60.0;

    public MatchResultDto toDto(MatchingProfile searcher, MatchingProfile candidate, MatchScore score, MatchQuality quality,
            RelationshipState relationship) {
        Student student = candidate.student();
        List<TimeSlot> shared = searcher.availability().sharedSlotsWith(candidate.availability());
        long sharedMinutes = shared.stream().mapToLong(TimeSlot::durationMinutes).sum();
        return new MatchResultDto(student.getId(), student.getName(), student.getSchool(), student.getProgramme(),
                student.getYearOfStudy(), student.getPreferredStudyMode(), student.getStudyGoals().stream().sorted().toList(),
                quality, breakdownOf(score), toTenthOfAnHour(sharedMinutes), shared.stream().map(MatchResultAssembler::toDto).toList(),
                relationship);
    }

    /** Every criterion in declaration order, so the bars always appear in the same order. */
    private static List<CriterionScoreDto> breakdownOf(MatchScore score) {
        return Arrays.stream(MatchingCriterion.values())
                .map(criterion -> new CriterionScoreDto(criterion, toHundredth(score.scoreFor(criterion))))
                .toList();
    }

    private static SharedSlotDto toDto(TimeSlot slot) {
        return new SharedSlotDto(slot.day(), slot.start(), slot.end());
    }

    private static double toTenthOfAnHour(long minutes) {
        return Math.round(minutes / MINUTES_PER_HOUR * 10) / 10.0;
    }

    private static double toHundredth(double score) {
        return Math.round(score * 100) / 100.0;
    }
}
