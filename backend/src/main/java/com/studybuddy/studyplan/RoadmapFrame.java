package com.studybuddy.studyplan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The consecutive blocks a roadmap covers, which Gemini fills and the validator checks its reply
 * against: one or more weeks, each with the student's weekly hours, or one or more of a session's
 * upcoming meetings, each as long as the session. A single meeting's entries are its timed agenda.
 */
public record RoadmapFrame(BlockKind kind, List<RoadmapBlock> blocks) {

    /** What a block is called in the prompt, and how its date is introduced. */
    public enum BlockKind {
        WEEK("Week", "from"),
        MEETING("Meeting", "on");

        private final String label;
        private final String datePreposition;

        BlockKind(String label, String datePreposition) {
            this.label = label;
            this.datePreposition = datePreposition;
        }

        public String label() {
            return label;
        }

        public String datePreposition() {
            return datePreposition;
        }
    }

    public RoadmapFrame {
        if (blocks.isEmpty()) throw new IllegalArgumentException("A roadmap needs at least one block");
        blocks = List.copyOf(blocks);
    }

    /** {@code count} weeks starting on {@code firstDay}, each holding the student's weekly study time. */
    public static RoadmapFrame weeks(LocalDate firstDay, int count, int minutesPerWeek) {
        List<RoadmapBlock> weeks = new ArrayList<>();
        for (int week = 1; week <= count; week++) {
            weeks.add(new RoadmapBlock(week, firstDay.plusWeeks(week - 1L), minutesPerWeek));
        }
        return new RoadmapFrame(BlockKind.WEEK, weeks);
    }

    /** One block per meeting date, in order, each as long as one meeting. */
    public static RoadmapFrame meetings(List<LocalDate> dates, int minutesPerMeeting) {
        List<RoadmapBlock> meetings = new ArrayList<>();
        for (int index = 0; index < dates.size(); index++) {
            meetings.add(new RoadmapBlock(index + 1, dates.get(index), minutesPerMeeting));
        }
        return new RoadmapFrame(BlockKind.MEETING, meetings);
    }

    public Optional<RoadmapBlock> block(int sequence) {
        return blocks.stream().filter(block -> block.sequence() == sequence).findFirst();
    }

    public LocalDate firstDate() {
        return blocks.getFirst().date();
    }

    /** Every block of a frame holds the same time, so the first block's is the frame's. */
    public int minutesPerBlock() {
        return blocks.getFirst().availableMinutes();
    }
}
