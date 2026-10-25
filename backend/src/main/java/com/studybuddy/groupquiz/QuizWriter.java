package com.studybuddy.groupquiz;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import com.studybuddy.course.Course;
import org.springframework.stereotype.Component;

/**
 * Gets a quiz from the AI: builds the prompt, sends it, and accepts the reply only if it passes the
 * output checks. Runs outside any database transaction, because the AI can take many seconds to
 * answer.
 */
@Component
public class QuizWriter {
    private final QuizPromptBuilder prompts;
    private final AiClient ai;
    private final QuizOutputValidator validator;

    public QuizWriter(QuizPromptBuilder prompts, AiClient ai, QuizOutputValidator validator) {
        this.prompts = prompts;
        this.ai = ai;
        this.validator = validator;
    }

    /**
     * The host is used only for their share of the daily AI allowance; it never reaches the AI.
     *
     * @throws AiUnavailableException when the AI is unavailable or its reply is rejected
     */
    public QuizDraft write(Long hostId, Course course, String topics) {
        String reply = ai.generateJson(hostId, prompts.build(course, topics));
        return validator.validate(reply).orElseThrow(AiUnavailableException::new);
    }
}
