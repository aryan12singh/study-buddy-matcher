package com.studybuddy.assistant;

import com.studybuddy.ai.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Works out what a message wants and which details it states: first with plain rules for the
 * clearest messages, which costs no AI call, and otherwise by asking the assistant's own AI. That is
 * the only AI call the assistant makes; everything it then tells the student comes from the app's
 * own services. Runs outside any database transaction, because the AI can take seconds to answer.
 */
@Component
public class IntentParser {
    private static final Logger LOG = LoggerFactory.getLogger(IntentParser.class);

    private final QuickIntentReader quick;
    private final AssistantPromptBuilder prompts;
    private final AssistantAi ai;
    private final AssistantOutputValidator validator;
    private final AssistantProperties properties;
    private final Clock clock;

    public IntentParser(QuickIntentReader quick, AssistantPromptBuilder prompts, AssistantAi ai,
            AssistantOutputValidator validator, AssistantProperties properties, Clock clock) {
        this.quick = quick;
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
        Optional<AssistantRequest> clear = quick.read(message);
        if (clear.isPresent()) {
            return clear.get();
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(properties.timeZone())));
        String reply = ai.generateJson(studentId, prompts.build(message, today));
        return validator.validate(reply).orElseThrow(() -> {
            LOG.warn("Gemini assistant reply rejected by the intent checks");
            return new AiUnavailableException();
        });
    }
}
