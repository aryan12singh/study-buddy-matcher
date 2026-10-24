package com.studybuddy.studyplan;

import com.studybuddy.common.InputRules;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns Gemini's roadmap reply into a {@link RoadmapDraft}, or rejects it. A reply is accepted only
 * if it has the agreed shape, every entry names a block of the frame, every block has between one
 * and the configured number of entries, a block's minutes fit its limit, and every text fits its
 * column. These are bounded checks on shape and size; they cannot prove the plan is good advice.
 */
@Component
public class StudyPlanOutputValidator {
    private final StudyPlanProperties properties;
    private final JsonMapper json;

    public StudyPlanOutputValidator(StudyPlanProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    public Optional<RoadmapDraft> validate(String raw, RoadmapFrame frame) {
        JsonNode reply;
        try {
            reply = json.readTree(raw == null ? "" : raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        Optional<List<String>> topics = sourceTopicsOf(reply.path("sourceTopics"));
        Optional<List<RoadmapDraft.Entry>> entries = entriesOf(reply.path("entries"));
        if (topics.isEmpty() || entries.isEmpty() || !fitsFrame(entries.get(), frame)) {
            return Optional.empty();
        }
        return Optional.of(new RoadmapDraft(topics.get(), inBlockOrder(entries.get())));
    }

    /** One to the configured number of distinct topics; a repeat (ignoring case) is dropped. */
    private Optional<List<String>> sourceTopicsOf(JsonNode node) {
        Optional<List<String>> titles = textsOf(node);
        if (titles.isEmpty()) {
            return Optional.empty();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> topics = new ArrayList<>();
        for (String title : titles.get()) {
            if (seen.add(title.toLowerCase(Locale.ROOT))) {
                topics.add(title);
            }
        }
        boolean fits = !topics.isEmpty() && topics.size() <= properties.maxSourceTopics();
        return fits ? Optional.of(topics) : Optional.empty();
    }

    private Optional<List<RoadmapDraft.Entry>> entriesOf(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return Optional.empty();
        }
        List<RoadmapDraft.Entry> entries = new ArrayList<>();
        for (JsonNode entry : node) {
            Optional<RoadmapDraft.Entry> parsed = entryOf(entry);
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            entries.add(parsed.get());
        }
        return Optional.of(entries);
    }

    private Optional<RoadmapDraft.Entry> entryOf(JsonNode entry) {
        JsonNode sequence = entry.path("sequence");
        JsonNode minutes = entry.path("plannedMinutes");
        Optional<String> title = textOf(entry.path("title"));
        // A missing task list counts as no tasks; anything else must be a list of short texts.
        Optional<List<String>> tasks = entry.path("tasks").isMissingNode() ? Optional.of(List.of()) : textsOf(entry.path("tasks"));
        if (!isWholeNumber(sequence) || !isWholeNumber(minutes) || minutes.asInt() <= 0 || title.isEmpty()
                || tasks.isEmpty() || tasks.get().size() > properties.maxTasksPerEntry()) {
            return Optional.empty();
        }
        return Optional.of(new RoadmapDraft.Entry(sequence.asInt(), title.get(), minutes.asInt(), tasks.get()));
    }

    /** Every block is covered, no entry names a block outside the frame, and each block's time fits. */
    private boolean fitsFrame(List<RoadmapDraft.Entry> entries, RoadmapFrame frame) {
        Map<Integer, Integer> entriesPerBlock = new HashMap<>();
        Map<Integer, Integer> minutesPerBlock = new HashMap<>();
        for (RoadmapDraft.Entry entry : entries) {
            if (frame.block(entry.sequence()).isEmpty()) {
                return false;
            }
            entriesPerBlock.merge(entry.sequence(), 1, Integer::sum);
            minutesPerBlock.merge(entry.sequence(), entry.plannedMinutes(), Integer::sum);
        }
        for (RoadmapBlock block : frame.blocks()) {
            int count = entriesPerBlock.getOrDefault(block.sequence(), 0);
            if (count == 0 || count > properties.maxEntriesPerBlock()
                    || minutesPerBlock.get(block.sequence()) > block.availableMinutes()) {
                return false;
            }
        }
        return true;
    }

    /** Sorted by block; entries within a block keep the order Gemini gave them. */
    private static List<RoadmapDraft.Entry> inBlockOrder(List<RoadmapDraft.Entry> entries) {
        return entries.stream().sorted(Comparator.comparingInt(RoadmapDraft.Entry::sequence)).toList();
    }

    private static Optional<List<String>> textsOf(JsonNode node) {
        if (!node.isArray()) {
            return Optional.empty();
        }
        List<String> texts = new ArrayList<>();
        for (JsonNode item : node) {
            Optional<String> text = textOf(item);
            if (text.isEmpty()) {
                return Optional.empty();
            }
            texts.add(text.get());
        }
        return Optional.of(texts);
    }

    /** A non-blank string that fits its column once trimmed. */
    private static Optional<String> textOf(JsonNode node) {
        if (!node.isString()) {
            return Optional.empty();
        }
        String text = node.asString().strip();
        return text.isEmpty() || text.length() > InputRules.TEXT_LIMIT ? Optional.empty() : Optional.of(text);
    }

    /** 60 or 60.0, but not "60", 60.5 or a number too large for an int. */
    private static boolean isWholeNumber(JsonNode node) {
        return node.isNumber() && node.canConvertToExactIntegral() && node.canConvertToInt();
    }
}
