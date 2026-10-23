package com.studybuddy.studyplan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The blocks Gemini must fill and the validator checks its reply against: a number of weeks, each
 * with the student's weekly hours, or a session's upcoming meetings, each as long as the session.
 * A one-off session is a single meeting, so its entries become that meeting's timed agenda.
 */
public record RoadmapFrame(BlockKind kind, List<RoadmapBlock> blocks) {

    /** What a block is called in the prompt. */
    public enum BlockKind {
        WEEK("Week"),
        MEETING("Meeting");

        private final String label;

        BlockKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public RoadmapFrame {
        if (blocks.isEmpty()) throw new IllegalArgumentException("A roadmap needs at least one block");
        blocks = List.copyOf(blocks);
    }

    /** Weeks 1 to {@code count}, each holding the student's weekly study time. */
    public static RoadmapFrame weeks(int count, int minutesPerWeek) {
        List<RoadmapBlock> weeks = new ArrayList<>();
        for (int week = 1; week <= count; week++) {
            weeks.add(new RoadmapBlock(week, null, minutesPerWeek));
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
}
