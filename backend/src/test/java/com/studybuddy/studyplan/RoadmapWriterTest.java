package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAttachment;
import com.studybuddy.ai.AiClient;
import com.studybuddy.ai.AiUnavailableException;
import com.studybuddy.course.Course;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadmapWriterTest {
    private static final Course IS442 = new Course("IS442", "Object Oriented Programming");
    private static final StudyPlanProperties LIMITS =
            new StudyPlanProperties(DataSize.ofMegabytes(10), 6, 4000, 100000, 40, 5, 5, 16, 40, "Asia/Singapore");
    private static final RoadmapFrame ONE_WEEK = RoadmapFrame.weeks(LocalDate.of(2026, 10, 23), 1, 120);
    private static final String GOOD_REPLY = """
            {"sourceTopics": ["Polymorphism"],
             "entries": [{"sequence": 1, "title": "Polymorphism", "plannedMinutes": 90, "tasks": ["Tutorial 4"]}]}""";

    /** Records what it was asked, then replies with fixed text or fails on demand. */
    private static final class FakeAi implements AiClient {
        private final String reply;
        final List<String> prompts = new ArrayList<>();
        final List<List<AiAttachment>> attachments = new ArrayList<>();
        final List<Long> students = new ArrayList<>();

        FakeAi(String reply) {
            this.reply = reply;
        }

        @Override
        public String generateJson(Long studentId, String prompt, List<AiAttachment> files) {
            students.add(studentId);
            prompts.add(prompt);
            attachments.add(files);
            if (reply == null) {
                throw new AiUnavailableException();
            }
            return reply;
        }
    }

    private static RoadmapWriter writer(FakeAi ai) {
        return new RoadmapWriter(new StudyPlanPromptBuilder(LIMITS), ai,
                new StudyPlanOutputValidator(LIMITS, JsonMapper.builder().build()));
    }

    @Test
    void returnsTheValidatedDraftAndSendsTheMaterialsFiles() {
        FakeAi ai = new FakeAi(GOOD_REPLY);
        AiAttachment slides = new AiAttachment("application/pdf", "%PDF-1.7".getBytes());
        StudyMaterial material = StudyMaterial.uploads(List.of("week4.pdf"), List.of(slides), "");

        RoadmapDraft draft = writer(ai).write(7L, IS442, null, ONE_WEEK, material);

        assertEquals(List.of("Polymorphism"), draft.sourceTopics());
        assertEquals(90, draft.entries().getFirst().plannedMinutes());
        assertEquals(List.of(7L), ai.students);
        assertSame(slides, ai.attachments.getFirst().getFirst());
        assertTrue(ai.prompts.getFirst().contains("Course: IS442 Object Oriented Programming"));
    }

    @Test
    void aReplyThatFailsTheChecksIsUnavailable() {
        FakeAi ai = new FakeAi("{\"sourceTopics\": [], \"entries\": []}");

        assertThrows(AiUnavailableException.class,
                () -> writer(ai).write(7L, IS442, null, ONE_WEEK, StudyMaterial.pastedTopics("Polymorphism")));
    }

    @Test
    void anUnavailableAiStaysUnavailable() {
        assertThrows(AiUnavailableException.class,
                () -> writer(new FakeAi(null)).write(7L, IS442, null, ONE_WEEK, StudyMaterial.pastedTopics("Polymorphism")));
    }
}
