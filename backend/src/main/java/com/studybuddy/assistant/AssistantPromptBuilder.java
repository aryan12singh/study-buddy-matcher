package com.studybuddy.assistant;

import com.studybuddy.ai.PromptTemplate;
import com.studybuddy.student.GroupSizePreference;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
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
    public static final String PROMPT_VERSION = "v2";
    private static final String TEMPLATE_PATH = "prompts/assistant-" + PROMPT_VERSION + ".txt";
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    /** A marker inside the student's message would end the message block early. */
    private static final Pattern MESSAGE_MARKER = Pattern.compile("</?message>", Pattern.CASE_INSENSITIVE);

    private final PromptTemplate template;

    public AssistantPromptBuilder() {
        this.template = PromptTemplate.load(TEMPLATE_PATH);
    }

    public String build(String message, LocalDate today) {
        Map<String, String> values = Map.of(
                "today", TODAY.format(today) + " (" + today + ")",
                "studyGoals", namesOf(StudyGoal.values()),
                "studyModes", namesOf(StudyMode.values()),
                "groupSizes", namesOf(GroupSizePreference.values()),
                "pages", pageList(),
                "message", MESSAGE_MARKER.matcher(message).replaceAll(""));
        return template.fill(values);
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
}
