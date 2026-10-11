package com.studybuddy.matching;

import com.studybuddy.profile.RelationshipState;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.util.List;

/**
 * One student in a match search's results. Carries a plain-language quality
 * label and a per-criterion breakdown rather than the weighted total, where the
 * searcher already stands with them, and never a contact number.
 */
public record MatchResultDto(
        Long studentId,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        StudyMode studyMode,
        List<StudyGoal> studyGoals,
        MatchQuality quality,
        List<CriterionScoreDto> breakdown,
        double sharedHoursPerWeek,
        List<SharedSlotDto> sharedSlots,
        RelationshipState relationship) {
}
