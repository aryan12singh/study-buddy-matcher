package com.studybuddy.assistant;

import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Fills the versioned assistant prompt template with today's date, the app's own enum values and
 * the student's message. Holds only that one message; never a name, email or anyone else's data.
 * Editing the wording once it is merged means adding a new template file and bumping
 * {@link #PROMPT_VERSION}.
 */
@Component
public class AssistantPromptBuilder {
    public static final String PROMPT_VERSION = "v1";
    private static final String TEMPLATE_PATH = "prompts/assistant-" + PROMPT_VERSION + ".txt";
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    /** A marker inside the student's message would end the message block early. */
    private static final Pattern MESSAGE_MARKER = Pattern.compile("</?message>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLOT = Pattern.compile("\\{\\{(\\w+)}}");

    private final String template;

    public AssistantPromptBuilder() {
        this.template = loadTemplate();
    }

    public String build(String message, LocalDate today) {
        Map<String, String> values = Map.of(
                "today", TODAY.format(today) + " (" + today + ")",
                "studyGoals", namesOf(StudyGoal.values()),
                "studyModes", namesOf(StudyMode.values()),
                "groupSizes", namesOf(GroupSizePreference.values()),
                "pages", pageList(),
                "message", MESSAGE_MARKER.matcher(message).replaceAll(""));
        // One pass over the template only, so the student's text is never searched for slots.
        return SLOT.matcher(template).replaceAll(slot -> {
            String value = values.get(slot.group(1));
            if (value == null) throw new IllegalStateException("Prompt template slot {{" + slot.group(1) + "}} has no value");
            return Matcher.quoteReplacement(value);
        });
    }

    /** One line per page: its name for the reply, then what it is for. */
    private static String pageList() {
        return Arrays.stream(AppPage.values())
                .map(page -> "  - " + page.name() + ": " + page.purpose())
                .collect(Collectors.joining("\n"));
    }

    private static String namesOf(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
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
