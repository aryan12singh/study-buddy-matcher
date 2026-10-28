package com.studybuddy.assistant;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;

import java.time.LocalDate;

/**
 * A student's message as the AI read it: what they want and the details they stated, each null
 * when the message did not say it. Checked by {@link AssistantOutputValidator}; the course code and
 * group name are still the student's own words until an intent resolves them against the database.
 * {@code meetingDate} is the study session meeting day they ask about, such as "Thursday".
 */
public record AssistantRequest(
        AssistantIntentType intent,
        String courseCode,
        StudyGoal studyGoal,
        StudyMode studyMode,
        GroupSizePreference groupSize,
        Integer minSharedHours,
        TimeOfDay timeOfDay,
        String groupName,
        Integer durationMinutes,
        LocalDate examDate,
        Integer hoursPerWeek,
        String topic,
        AppPage page,
        LocalDate meetingDate) {

    /** A request the assistant could not classify, with no details. */
    public static AssistantRequest unknown() {
        return new AssistantRequest(AssistantIntentType.UNKNOWN, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
