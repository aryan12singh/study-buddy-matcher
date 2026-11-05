package com.studybuddy.studyplan;

import com.studybuddy.ai.PromptTemplate;
import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fills the versioned roadmap prompt template. Holds only the course, the goal, the blocks to plan
 * and the material the student chose to send; never a name, email or anyone else's data. Editing
 * the wording once it is merged means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class StudyPlanPromptBuilder {
    public static final String PROMPT_VERSION = "v2";
    private static final String TEMPLATE_PATH = "prompts/study-plan-" + PROMPT_VERSION + ".txt";
    private static final DateTimeFormatter BLOCK_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);

    private final StudyPlanProperties properties;
    private final PromptTemplate template;

    public StudyPlanPromptBuilder(StudyPlanProperties properties) {
        this.properties = properties;
        this.template = PromptTemplate.load(TEMPLATE_PATH);
    }

    public String build(Course course, StudyGoal goal, RoadmapFrame frame, StudyMaterial material) {
        Map<String, String> values = Map.of(
                "course", course.getCode() + " " + course.getName(),
                "goal", goal == null ? "not specified" : describe(goal),
                "maxTopics", String.valueOf(properties.maxSourceTopics()),
                "maxEntries", String.valueOf(properties.maxEntriesPerBlock()),
                "maxTasks", String.valueOf(properties.maxTasksPerEntry()),
                "blocks", describe(frame),
                "materialSource", material.describeSource("topics the student typed", true),
                "material", material.promptBlock());
        return template.fill(values);
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
}
