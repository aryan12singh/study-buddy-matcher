package com.studybuddy.quiz;

import com.studybuddy.ai.PromptTemplate;
import com.studybuddy.course.Course;
import com.studybuddy.studyplan.StudyMaterial;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fills the versioned quiz prompt template. Holds only the course, the optional focus and the
 * material the student chose to send; never a name, email or any member's data. Editing the
 * wording once it is merged means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class QuizPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/quiz-" + PROMPT_VERSION + ".txt";

    private final QuizProperties properties;
    private final PromptTemplate template;

    public QuizPromptBuilder(QuizProperties properties) {
        this.properties = properties;
        this.template = PromptTemplate.load(TEMPLATE_PATH);
    }

    /** @param focus what to ask mostly about, or null to cover the whole material */
    public String build(Course course, String focus, StudyMaterial material) {
        Map<String, String> values = Map.of(
                "course", course.getCode() + " " + course.getName(),
                "focus", focus == null ? "none; cover the whole material" : StudyMaterial.withoutMarkers(focus),
                "questionCount", String.valueOf(properties.questionCount()),
                "promptLimit", String.valueOf(QuizQuestion.PROMPT_LIMIT),
                "optionLimit", String.valueOf(QuizQuestion.OPTION_LIMIT),
                "explanationLimit", String.valueOf(QuizQuestion.EXPLANATION_LIMIT),
                "materialSource", material.describeSource("notes the student pasted", false),
                "material", material.promptBlock());
        return template.fill(values);
    }
}
