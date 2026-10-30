package com.studybuddy.seed;

import com.studybuddy.student.AvailabilitySlotDto;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import com.studybuddy.studygroup.GroupAvailabilitySlotDto;
import tools.jackson.databind.JsonNode;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The demo dataset read from {@code demo-seed/demo-data.json}: the courses, the students and
 * their preferences, and the history between them (requests, connections, groups, a deactivated
 * account, study sessions, quizzes and study plans). Students, groups and sessions refer to shared
 * weekly time blocks by name, so timetables overlap the way real campus timetables do.
 *
 * @param presenter   handle of the account the live demo signs in as
 * @param courses     courses to create when their code is missing
 * @param timeBlocks  named weekly blocks, e.g. {@code WED_EVE}
 * @param students    the demo students, identified by handle
 * @param requests    match requests to replay, in order
 * @param groups      study groups to form, in order
 * @param deactivated handles of students an administrator deactivates
 * @param sessions    group and buddy sessions to schedule, starting at their block's next time
 * @param quizzes     quizzes to start from questions written in advance, then take
 * @param plans       study plans to create from roadmaps written in advance
 * @param forest      finished study room visits that grew trees, so the demo forest is not empty
 */
public record DemoData(
        String presenter,
        List<CourseEntry> courses,
        Map<String, TimeBlock> timeBlocks,
        List<StudentEntry> students,
        List<RequestEntry> requests,
        List<GroupEntry> groups,
        List<String> deactivated,
        List<SessionEntry> sessions,
        List<QuizEntry> quizzes,
        List<PlanEntry> plans,
        List<ForestEntry> forest) {

    public List<AvailabilitySlotDto> studentSlots(List<String> blockNames) {
        return blockNames.stream().map(this::block)
                .map(block -> new AvailabilitySlotDto(block.day(), block.start(), block.end())).toList();
    }

    public List<GroupAvailabilitySlotDto> groupSlots(List<String> blockNames) {
        return blockNames.stream().map(this::block)
                .map(block -> new GroupAvailabilitySlotDto(block.day(), block.start(), block.end())).toList();
    }

    public TimeBlock block(String name) {
        TimeBlock block = timeBlocks.get(name);
        if (block == null) {
            throw new IllegalStateException("Demo data refers to unknown time block " + name);
        }
        return block;
    }

    public record CourseEntry(String code, String name) {
    }

    public record TimeBlock(DayOfWeek day, LocalTime start, LocalTime end) {

        /** The block's next start strictly after {@code now}, in the same wall-clock time. */
        public LocalDateTime nextStartAfter(LocalDateTime now) {
            LocalDateTime start = now.toLocalDate().with(TemporalAdjusters.nextOrSame(day)).atTime(this.start);
            return start.isAfter(now) ? start : start.plusWeeks(1);
        }
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

    /**
     * A weekly session scheduled by {@code organiser}, either for a group they belong to or with a
     * connected {@code buddy}; exactly one of the two is set.
     */
    public record SessionEntry(String organiser, String group, String buddy, String title, String block, int weeks,
            String timeZone, String agenda, String location) {
    }

    /**
     * A quiz started by {@code by}: for a group they may host when {@code group} is set, otherwise a
     * personal practice quiz for {@code course}. {@code questions} has the shape of Gemini's reply,
     * so it passes the same checks. Each taker answers in question order; {@code close} closes a
     * group quiz early so its answers can be reviewed.
     */
    public record QuizEntry(String by, String group, String course, String focus, String notes, JsonNode questions,
            List<TakerEntry> takers, boolean close) {
    }

    /** A student's chosen option for each question, in question order. */
    public record TakerEntry(String handle, List<Integer> answers) {
    }

    /**
     * A personal study plan up to an exam {@code examInDays} away. {@code roadmap} has the shape of
     * Gemini's reply and must cover exactly the weeks up to the exam. Entries named in
     * {@code completed} are ticked off.
     */
    public record PlanEntry(String student, String course, StudyGoal goal, int hoursPerWeek, int examInDays,
            String topics, JsonNode roadmap, List<String> completed) {
    }

    /**
     * A finished visit by {@code student} to {@code group}'s study room, {@code daysAgo} days back,
     * starting at {@code startTime} in the forest zone, with {@code focusedMinutes} of focus.
     */
    public record ForestEntry(String student, String group, int daysAgo, LocalTime startTime, int focusedMinutes) {
    }

    /** PENDING stays unanswered; CONNECTED is accepted; ENDED is accepted, then ended by the receiver. */
    public enum RequestOutcome {
        PENDING, CONNECTED, ENDED
    }
}
