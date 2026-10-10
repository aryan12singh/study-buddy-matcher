package com.studybuddy.matching;

/**
 * One student in a match search's results. Carries a plain-language quality
 * label rather than the raw score, and never a contact number.
 */
public record MatchResultDto(
        Long studentId,
        String name,
        String school,
        String programme,
        Integer yearOfStudy,
        MatchQuality quality,
        double sharedHoursPerWeek) {
}
