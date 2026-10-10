package com.studybuddy.matching;

import com.studybuddy.matching.scoring.MatchingProfile;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.ProfileProperties;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;

/**
 * Decides whether a candidate belongs in a search's results before scoring: they
 * must fit the searched course and goal and every filter that was set. The
 * minimum quality filter needs a score, so the search applies it afterwards.
 */
@Component
public class MatchSearchFilter {

    private static final int MINUTES_PER_HOUR = 60;

    private final ProfileProperties profileLimits;

    public MatchSearchFilter(ProfileProperties profileLimits) {
        this.profileLimits = profileLimits;
    }

    public boolean accepts(MatchSearchCriteria search, MatchingProfile searcher, MatchingProfile candidate) {
        Student student = candidate.student();
        return takesCourse(search.courseId(), student)
                && hasGoal(search, student)
                && suitsStudyMode(search.studyMode(), student.getPreferredStudyMode())
                && suitsGroupSize(search.groupSize(), student)
                && sharesEnoughTime(search.minSharedHours(), searcher, candidate);
    }

    private static boolean takesCourse(Long courseId, Student student) {
        if (courseId == null) {
            return true;
        }
        boolean isTarget = student.getTargetCourse() != null && courseId.equals(student.getTargetCourse().getId());
        return isTarget || student.getCoursesTaken().stream().anyMatch(course -> courseId.equals(course.getId()));
    }

    private static boolean hasGoal(MatchSearchCriteria search, Student student) {
        return search.studyGoal() == null || student.getStudyGoals().contains(search.studyGoal());
    }

    /** Asking for {@code EITHER} filters nothing; a candidate who chose {@code EITHER} suits any mode. */
    private static boolean suitsStudyMode(StudyMode wanted, StudyMode theirs) {
        if (wanted == null || wanted == StudyMode.EITHER) {
            return true;
        }
        return theirs == wanted || theirs == StudyMode.EITHER;
    }

    /**
     * Keeps candidates whose preferred size range overlaps the one asked for. Asking for
     * {@code EITHER} filters nothing; a candidate with no stated range cannot be confirmed.
     */
    private boolean suitsGroupSize(GroupSizePreference wanted, Student student) {
        if (wanted == null || wanted == GroupSizePreference.EITHER) {
            return true;
        }
        Integer theirMin = student.getPreferredGroupSizeMin();
        Integer theirMax = student.getPreferredGroupSizeMax();
        if (theirMin == null || theirMax == null) {
            return false;
        }
        return theirMin <= wanted.maximumSize(profileLimits.groupSizeMax()) && wanted.minimumSize() <= theirMax;
    }

    private static boolean sharesEnoughTime(Integer minSharedHours, MatchingProfile searcher, MatchingProfile candidate) {
        if (minSharedHours == null) {
            return true;
        }
        long sharedMinutes = searcher.availability().sharedMinutesWith(candidate.availability());
        return sharedMinutes >= (long) minSharedHours * MINUTES_PER_HOUR;
    }
}
