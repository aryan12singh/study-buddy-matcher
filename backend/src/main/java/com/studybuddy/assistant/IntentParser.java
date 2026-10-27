package com.studybuddy.assistant;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Asks the AI what a message wants and which details it states. This is the only AI call the
 * assistant makes; everything it then tells the student comes from the app's own services.
 * Runs outside any database transaction, because the AI can take seconds to answer.
 */
@Component
public class IntentParser {
    private static final Logger LOG = LoggerFactory.getLogger(IntentParser.class);

    private final AssistantPromptBuilder prompts;
    private final AiClient ai;
    private final AssistantOutputValidator validator;
    private final AssistantProperties properties;
    private final Clock clock;

    public IntentParser(AssistantPromptBuilder prompts, AiClient ai, AssistantOutputValidator validator,
            AssistantProperties properties, Clock clock) {
        this.prompts = prompts;
        this.ai = ai;
        this.validator = validator;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * The student is used only for their share of the daily AI allowance; it never reaches the AI.
     *
     * @throws AiUnavailableException when the AI is unavailable or its reply is rejected
     */
    public AssistantRequest parse(Long studentId, String message) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(properties.timeZone())));
        String reply = ai.generateJson(studentId, prompts.build(message, today));
        return validator.validate(reply).orElseThrow(() -> {
            LOG.warn("Gemini assistant reply rejected by the intent checks");
            return new AiUnavailableException();
        });
    }
}
