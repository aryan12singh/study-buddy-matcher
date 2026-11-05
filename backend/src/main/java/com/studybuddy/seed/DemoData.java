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
 * @param sessions    group and buddy sessions to schedule, each around its block's next time
 * @param quizzes     quizzes to start from questions written in advance, then take
 * @param plans       study plans to create from roadmaps written in advance
 * @param sessionPlans session roadmaps to create from roadmaps written in advance
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
        List<SessionPlanEntry> sessionPlans,
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

    /**
     * A group, its accepted members and its still-pending applicants. A group formed
     * {@code formedWeeksAgo} weeks back dates its members' joining to then, so its earlier
     * meetings count towards their planned-session streak.
     */
    public record GroupEntry(String leader, String name, String description, String course, int maxSize,
            Set<StudyGoal> goals, StudyMode studyMode, List<String> meets, List<String> members,
            List<String> applicants, boolean closed, int formedWeeksAgo) {
    }

    /**
     * A weekly session scheduled by {@code organiser}, either for a group they belong to or with a
     * connected {@code buddy}; exactly one of the two is set. It starts at its block's next time, or
     * {@code weeksAgo} weeks before that for a series already under way.
     */
    public record SessionEntry(String organiser, String group, String buddy, String title, String block, int weeksAgo,
            int weeks, String timeZone, String agenda, String location) {
    }

    /**
     * A quiz started by {@code by}: for a group they may host when {@code group} is set, otherwise a
     * personal practice quiz for {@code course}. A group quiz may belong to the group's session
     * titled {@code session}. {@code questions} has the shape of Gemini's reply, so it passes the
     * same checks. Each taker answers in question order; {@code close} closes a group quiz early so
     * its answers can be reviewed. A personal quiz with no takers stays open to take in the demo.
     */
    public record QuizEntry(String by, String group, String session, String course, String focus, String notes,
            JsonNode questions, List<TakerEntry> takers, boolean close) {
    }

    /** A student's chosen option for each question, in question order, and how long they took. */
    public record TakerEntry(String handle, List<Integer> answers, int secondsTaken) {
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
     * A roadmap for the upcoming meetings of the session titled {@code session}, made by its
     * {@code organiser}. {@code roadmap} has the shape of Gemini's reply, numbering the meetings
     * from the next one; meetings the session no longer has are left out.
     */
    public record SessionPlanEntry(String organiser, String session, StudyGoal goal, String topics, JsonNode roadmap) {
    }

    /**
     * A finished visit by {@code student} to {@code group}'s study room, {@code daysAgo} days back,
     * starting at {@code startTime} in the forest zone, with {@code focusedMinutes} of focus.
     */
    public record ForestEntry(String student, String group, int daysAgo, LocalTime startTime, int focusedMinutes) {
    }

    /**
     * PENDING stays unanswered; CONNECTED is accepted; ENDED is accepted, then ended by the receiver;
     * DECLINED is declined by the receiver; CANCELLED is withdrawn by the sender.
     */
    public enum RequestOutcome {
        PENDING, CONNECTED, ENDED, DECLINED, CANCELLED
    }
}
