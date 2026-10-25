package com.studybuddy.groupquiz;

import com.studybuddy.course.Course;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills the versioned quiz prompt template. Holds only the group's course and the topics the host
 * typed; never a name, email or any member's data. Editing the wording once it is merged means
 * adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class QuizPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/group-quiz-" + PROMPT_VERSION + ".txt";
    /** A marker inside the host's text would end the topics block early. */
    private static final Pattern TOPICS_MARKER = Pattern.compile("</?topics>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT = Pattern.compile("\\{\\{(\\w+)}}");

    private final QuizProperties properties;
    private final String template;

    public QuizPromptBuilder(QuizProperties properties) {
        this.properties = properties;
        this.template = loadTemplate();
    }

    public String build(Course course, String topics) {
        Map<String, String> values = Map.of(
                "course", course.getCode() + " " + course.getName(),
                "questionCount", String.valueOf(properties.questionCount()),
                "promptLimit", String.valueOf(QuizQuestion.PROMPT_LIMIT),
                "optionLimit", String.valueOf(QuizQuestion.OPTION_LIMIT),
                "explanationLimit", String.valueOf(QuizQuestion.EXPLANATION_LIMIT),
                "topics", TOPICS_MARKER.matcher(topics).replaceAll(""));
        // One pass over the template only, so the host's text is never searched for slots,
        // whatever it contains.
        return SLOT.matcher(template).replaceAll(slot -> {
            String value = values.get(slot.group(1));
            if (value == null) throw new IllegalStateException("Prompt template slot {{" + slot.group(1) + "}} has no value");
            return Matcher.quoteReplacement(value);
        });
    }

    /** Git on Windows may check the template out with \r\n line endings; the prompt is the same everywhere. */
    private static String loadTemplate() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException missing) {
            throw new IllegalStateException("Prompt template " + TEMPLATE_PATH + " is missing", missing);
        }
    }
}
