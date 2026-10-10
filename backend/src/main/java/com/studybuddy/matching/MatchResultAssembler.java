package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

@Component
public class MatchResultAssembler {

    private static final double MINUTES_PER_HOUR = 60.0;

    public MatchResultDto toDto(MatchingProfile searcher, MatchingProfile candidate, MatchQuality quality) {
        Student student = candidate.student();
        long sharedMinutes = searcher.availability().sharedMinutesWith(candidate.availability());
        return new MatchResultDto(student.getId(), student.getName(), student.getSchool(), student.getProgramme(),
                student.getYearOfStudy(), quality, toTenthOfAnHour(sharedMinutes));
    }

    private static double toTenthOfAnHour(long minutes) {
        return Math.round(minutes / MINUTES_PER_HOUR * 10) / 10.0;
    }
}
