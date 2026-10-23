package com.studybuddy.studyplan;

import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fills the versioned roadmap prompt template. Holds only the course, the goal, the blocks to plan
 * and the material the student chose to send; never a name, email or anyone else's data. Editing
 * the wording once it is merged means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class StudyPlanPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/study-plan-" + PROMPT_VERSION + ".txt";
    private static final DateTimeFormatter BLOCK_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    /** A marker inside the student's text or file names would end the material block early. */
    private static final Pattern MATERIAL_MARKER = Pattern.compile("</?material>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT = Pattern.compile("\\{\\{(\\w+)}}");

    private final StudyPlanProperties properties;
    private final String template;

    public StudyPlanPromptBuilder(StudyPlanProperties properties) {
        this.properties = properties;
        this.template = loadTemplate();
    }

    public String build(Course course, StudyGoal goal, RoadmapFrame frame, StudyMaterial material) {
        Map<String, String> values = Map.of(
                "course", course.getCode() + " " + course.getName(),
                "goal", goal == null ? "not specified" : describe(goal),
                "maxTopics", String.valueOf(properties.maxSourceTopics()),
                "maxEntries", String.valueOf(properties.maxEntriesPerBlock()),
                "maxTasks", String.valueOf(properties.maxTasksPerEntry()),
                "blocks", describe(frame),
                "materialSource", describeSource(material),
                "material", material.text().isEmpty() ? "" : "<material>\n" + withoutMarkers(material.text()) + "\n</material>");
        // One pass over the template only, so a file name or text the student wrote is never
        // searched for slots, whatever it contains.
        return SLOT.matcher(template).replaceAll(slot -> {
            String value = values.get(slot.group(1));
            if (value == null) throw new IllegalStateException("Prompt template slot {{" + slot.group(1) + "}} has no value");
            return Matcher.quoteReplacement(value);
        });
    }

    /** Says where the material is: attached PDFs, text below, or both, with files in upload order. */
    private static String describeSource(StudyMaterial material) {
        if (material.source() == StudyMaterialSource.PASTE) {
            return "the topics the student typed between <material> and </material> below";
        }
        String files = "these files, in the order the student gave them: "
                + withoutMarkers(String.join(", ", material.filenames())) + ".";
        boolean hasPdf = !material.attachments().isEmpty();
        boolean hasText = !material.text().isEmpty();
        if (hasPdf && hasText) {
            return files + " The PDF files are attached in that order, and the text of each .txt or .md file is"
                    + " between <material> and </material> below, starting with \"File:\" and its name";
        }
        return hasPdf ? files + " They are attached in that order"
                : files + " Their text is between <material> and </material> below, each starting with \"File:\" and its name";
    }

    /** EXAM_PREPARATION reads as "exam preparation". */
    private static String describe(StudyGoal goal) {
        return goal.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String describe(RoadmapFrame frame) {
        return frame.blocks().stream()
                .map(block -> "- Block " + block.sequence() + " (" + frame.kind().label() + " " + block.sequence() + " "
                        + frame.kind().datePreposition() + " " + BLOCK_DATE.format(block.date())
                        + "): up to " + block.availableMinutes() + " minutes")
                .collect(Collectors.joining("\n"));
    }

    private static String withoutMarkers(String text) {
        return MATERIAL_MARKER.matcher(text).replaceAll("");
    }

    private static String loadTemplate() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new IllegalStateException("Prompt template " + TEMPLATE_PATH + " is missing", missing);
        }
    }
}
