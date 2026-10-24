package com.studybuddy.studyplan;

import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;
import org.springframework.stereotype.Component;

/**
 * Gets a roadmap from the AI: builds the prompt, sends it with any attached files, and accepts the
 * reply only if it passes the output checks. Runs outside any database transaction, because the AI
 * can take many seconds to answer.
 */
@Component
public class RoadmapWriter {
    private final StudyPlanPromptBuilder prompts;
    private final AiClient ai;
    private final StudyPlanOutputValidator validator;

    public RoadmapWriter(StudyPlanPromptBuilder prompts, AiClient ai, StudyPlanOutputValidator validator) {
        this.prompts = prompts;
        this.ai = ai;
        this.validator = validator;
    }

    /**
     * The student is used only for their share of the daily AI allowance; it never reaches the AI.
     *
     * @throws AiUnavailableException when the AI is unavailable or its reply is rejected
     */
    public RoadmapDraft write(Long studentId, Course course, StudyGoal goal, RoadmapFrame frame, StudyMaterial material) {
        String reply = ai.generateJson(studentId, prompts.build(course, goal, frame, material), material.attachments());
        return validator.validate(reply, frame).orElseThrow(AiUnavailableException::new);
    }
}
