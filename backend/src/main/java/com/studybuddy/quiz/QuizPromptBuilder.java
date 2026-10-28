package com.studybuddy.quiz;

import com.studybuddy.course.Course;
import com.studybuddy.studyplan.StudyMaterial;
import com.studybuddy.studyplan.StudyMaterialSource;
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
 * Fills the versioned quiz prompt template. Holds only the course, the optional focus and the
 * material the student chose to send; never a name, email or any member's data. Editing the
 * wording once it is merged means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class QuizPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/quiz-" + PROMPT_VERSION + ".txt";
    /** A marker inside the student's text or file names would end the material block early. */
    private static final Pattern MATERIAL_MARKER = Pattern.compile("</?material>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT = Pattern.compile("\\{\\{(\\w+)}}");

    private final QuizProperties properties;
    private final String template;

    public QuizPromptBuilder(QuizProperties properties) {
        this.properties = properties;
        this.template = loadTemplate();
    }

    /** @param focus what to ask mostly about, or null to cover the whole material */
    public String build(Course course, String focus, StudyMaterial material) {
        Map<String, String> values = Map.of(
                "course", course.getCode() + " " + course.getName(),
                "focus", focus == null ? "none; cover the whole material" : withoutMarkers(focus),
                "questionCount", String.valueOf(properties.questionCount()),
                "promptLimit", String.valueOf(QuizQuestion.PROMPT_LIMIT),
                "optionLimit", String.valueOf(QuizQuestion.OPTION_LIMIT),
                "explanationLimit", String.valueOf(QuizQuestion.EXPLANATION_LIMIT),
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
            return "the notes the student pasted between <material> and </material> below";
        }
        String files = "these files: " + withoutMarkers(String.join(", ", material.filenames())) + ".";
        boolean hasPdf = !material.attachments().isEmpty();
        boolean hasText = !material.text().isEmpty();
        if (hasPdf && hasText) {
            return files + " The PDF files are attached, and the text of each .txt or .md file is between"
                    + " <material> and </material> below, starting with \"File:\" and its name";
        }
        return hasPdf ? files + " They are attached"
                : files + " Their text is between <material> and </material> below, each starting with \"File:\" and its name";
    }

    private static String withoutMarkers(String text) {
        return MATERIAL_MARKER.matcher(text).replaceAll("");
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
