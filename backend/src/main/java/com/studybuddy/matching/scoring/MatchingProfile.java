package com.studybuddy.matching.scoring;

import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.student.Student;

/**
 * Everything the scorers need to know about one student. Availability lives in
 * its own table rather than on {@link Student}, so it is loaded once and carried
 * here instead of each scorer querying for it.
 */
public record MatchingProfile(Student student, WeeklyAvailability availability) {
}
