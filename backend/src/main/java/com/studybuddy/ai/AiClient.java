package com.studybuddy.ai;

import java.util.List;

/**
 * Asks the AI to answer a prompt with JSON. Features depend on this interface rather than on
 * Gemini, so their tests can supply a fake that returns fixed replies or fails on demand.
 */
public interface AiClient {

    /**
     * Returns the reply text, which the prompt asked to be JSON; the caller still parses and
     * validates it. The student is used only for their share of the daily allowance and never
     * reaches the AI.
     *
     * @throws AiUnavailableException when the assistant is off, over quota, rate limited, timed out
     *         or the reply is unusable
     */
    String generateJson(Long studentId, String prompt, List<AiAttachment> attachments);

    default String generateJson(Long studentId, String prompt) {
        return generateJson(studentId, prompt, List.of());
    }
}
