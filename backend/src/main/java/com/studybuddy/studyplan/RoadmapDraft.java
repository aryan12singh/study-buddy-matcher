package com.studybuddy.studyplan;

import java.util.List;
import java.util.Map;

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

    /**
     * The entries of the blocks named in {@code newSequences}, each given its block's new number;
     * entries of any other block are left out.
     */
    public RoadmapDraft renumbered(Map<Integer, Integer> newSequences) {
        return new RoadmapDraft(sourceTopics, entries.stream()
                .filter(entry -> newSequences.containsKey(entry.sequence()))
                .map(entry -> new Entry(newSequences.get(entry.sequence()), entry.title(), entry.plannedMinutes(), entry.tasks()))
                .toList());
    }

    /** What to study in one block; {@code sequence} names the week or meeting. */
    public record Entry(int sequence, String title, int plannedMinutes, List<String> tasks) {
        public Entry {
            tasks = List.copyOf(tasks);
        }
    }
}
