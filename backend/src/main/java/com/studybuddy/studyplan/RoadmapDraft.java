package com.studybuddy.studyplan;

import java.util.List;

/**
 * A roadmap Gemini wrote that has passed {@link StudyPlanOutputValidator}: the topics found in the
 * material, and the entries in block order. Not saved until the service turns it into a
 * {@link StudyPlan}.
 */
public record RoadmapDraft(List<String> sourceTopics, List<Entry> entries) {

    public RoadmapDraft {
        sourceTopics = List.copyOf(sourceTopics);
        entries = List.copyOf(entries);
    }

    /** What to study in one block; {@code sequence} names the week or meeting. */
    public record Entry(int sequence, String title, int plannedMinutes, List<String> tasks) {
        public Entry {
            tasks = List.copyOf(tasks);
        }
    }
}
