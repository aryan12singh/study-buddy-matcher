package com.studybuddy.seed;

import com.studybuddy.student.AvailabilitySlotDto;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import com.studybuddy.studygroup.GroupAvailabilitySlotDto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The demo dataset read from {@code demo-seed/demo-data.json}: the courses, the students and
 * their preferences, and the history between them (requests, connections, groups and a
 * deactivated account). Students and groups refer to shared weekly time blocks by name, so
 * timetables overlap the way real campus timetables do.
 *
 * @param presenter   handle of the account the live demo signs in as
 * @param courses     courses to create when their code is missing
 * @param timeBlocks  named weekly blocks, e.g. {@code WED_EVE}
 * @param students    the demo students, identified by handle
 * @param requests    match requests to replay, in order
 * @param groups      study groups to form, in order
 * @param deactivated handles of students an administrator deactivates
 */
public record DemoData(
        String presenter,
        List<CourseEntry> courses,
        Map<String, TimeBlock> timeBlocks,
        List<StudentEntry> students,
        List<RequestEntry> requests,
        List<GroupEntry> groups,
        List<String> deactivated) {

    public List<AvailabilitySlotDto> studentSlots(List<String> blockNames) {
        return blockNames.stream().map(this::block)
                .map(block -> new AvailabilitySlotDto(block.day(), block.start(), block.end())).toList();
    }

    public List<GroupAvailabilitySlotDto> groupSlots(List<String> blockNames) {
        return blockNames.stream().map(this::block)
                .map(block -> new GroupAvailabilitySlotDto(block.day(), block.start(), block.end())).toList();
    }

    private TimeBlock block(String name) {
        TimeBlock block = timeBlocks.get(name);
        if (block == null) {
            throw new IllegalStateException("Demo data refers to unknown time block " + name);
        }
        return block;
    }

    public record CourseEntry(String code, String name) {
    }

    public record TimeBlock(DayOfWeek day, LocalTime start, LocalTime end) {
    }

    public record StudentEntry(String handle, String name, String school, String programme, int year,
            String contactNumber, List<String> courses, String targetCourse, StudyMode studyMode,
            GroupSizePreference groupSize, Set<StudyGoal> goals, List<String> availability) {
    }

    /** A request from one student to another and how far it got. */
    public record RequestEntry(String from, String to, String message, String course, RequestOutcome outcome) {
    }

    /** A group, its accepted members and its still-pending applicants. */
    public record GroupEntry(String leader, String name, String description, String course, int maxSize,
            Set<StudyGoal> goals, StudyMode studyMode, List<String> meets, List<String> members,
            List<String> applicants, boolean closed) {
    }

    /** PENDING stays unanswered; CONNECTED is accepted; ENDED is accepted, then ended by the receiver. */
    public enum RequestOutcome {
        PENDING, CONNECTED, ENDED
    }
}
