package com.studybuddy.quiz;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import com.studybuddy.course.Course;
import com.studybuddy.studyplan.StudyMaterial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gets a quiz from the AI: builds the prompt, sends it with any attached PDFs, and accepts the reply
 * only if it passes the output checks. Runs outside any database transaction, because the AI can
 * take many seconds to answer.
 */
@Component
public class QuizWriter {
    private static final Logger LOG = LoggerFactory.getLogger(QuizWriter.class);
    private final QuizPromptBuilder prompts;
    private final AiClient ai;
    private final QuizOutputValidator validator;

    public QuizWriter(QuizPromptBuilder prompts, AiClient ai, QuizOutputValidator validator) {
        this.prompts = prompts;
        this.ai = ai;
        this.validator = validator;
    }

    /**
     * The student is used only for their share of the daily AI allowance; it never reaches the AI.
     *
     * @throws AiUnavailableException when the AI is unavailable or its reply is rejected
     */
    public QuizDraft write(Long studentId, Course course, String focus, StudyMaterial material) {
        String reply = ai.generateJson(studentId, prompts.build(course, focus, material), material.attachments());
        return validator.validate(reply).orElseThrow(() -> {
            LOG.warn("Gemini quiz reply rejected by the quiz checks");
            return new AiUnavailableException();
        });
    }
}
