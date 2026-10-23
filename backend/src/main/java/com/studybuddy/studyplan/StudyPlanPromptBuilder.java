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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fills the versioned roadmap prompt template. Holds only the course, the goal, the blocks to plan
 * and the material the student chose to send; never a name, email or anyone else's data. Editing
 * the wording means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class StudyPlanPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/study-plan-" + PROMPT_VERSION + ".txt";
    private static final DateTimeFormatter MEETING_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    /** A closing marker inside the student's text would end the material block early. */
    private static final Pattern MATERIAL_MARKER = Pattern.compile("</?material>", Pattern.CASE_INSENSITIVE);

    private final StudyPlanProperties properties;
    private final String template;

    public StudyPlanPromptBuilder(StudyPlanProperties properties) {
        this.properties = properties;
        this.template = loadTemplate();
    }

    public String build(Course course, StudyGoal goal, RoadmapFrame frame, StudyMaterial material) {
        boolean attached = !material.attachments().isEmpty();
        return template
                .replace("{{course}}", course.getCode() + " " + course.getName())
                .replace("{{goal}}", goal == null ? "not specified" : describe(goal))
                .replace("{{materialSource}}", attached ? "the attached PDF file" : "the text between <material> and </material> below")
                .replace("{{maxTopics}}", String.valueOf(properties.maxSourceTopics()))
                .replace("{{maxEntries}}", String.valueOf(properties.maxEntriesPerBlock()))
                .replace("{{maxTasks}}", String.valueOf(properties.maxTasksPerEntry()))
                .replace("{{blocks}}", describe(frame))
                // Last, so text the student wrote is never searched for the slots above.
                .replace("{{material}}", attached ? "" : "<material>\n" + MATERIAL_MARKER.matcher(material.text()).replaceAll("") + "\n</material>");
    }

    /** EXAM_PREPARATION reads as "exam preparation". */
    private static String describe(StudyGoal goal) {
        return goal.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String describe(RoadmapFrame frame) {
        return frame.blocks().stream()
                .map(block -> "- Block " + block.sequence() + " (" + frame.kind().label() + " " + block.sequence()
                        + (block.date() == null ? "" : " on " + MEETING_DATE.format(block.date()))
                        + "): up to " + block.availableMinutes() + " minutes")
                .collect(Collectors.joining("\n"));
    }

    private static String loadTemplate() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new IllegalStateException("Prompt template " + TEMPLATE_PATH + " is missing", missing);
        }
    }
}
