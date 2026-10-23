package com.studybuddy.studyplan;

import com.studybuddy.ai.AiAssistantProperties;
import com.studybuddy.ai.AiQuota;
import com.studybuddy.ai.GeminiClient;
import com.studybuddy.course.Course;
import com.studybuddy.student.StudyGoal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.util.unit.DataSize;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that real Gemini, given the real roadmap prompt, writes a reply the validator accepts, and
 * prints the roadmap so a person can judge it. Skipped unless {@code GEMINI_LIVE_TEST_KEY} is set.
 * {@code GEMINI_LIVE_TEST_PDF} points at a PDF of course notes; without it a short topic list is
 * used. Each test makes one call from that key's free allowance. Run by hand:
 * <pre>
 * $env:GEMINI_LIVE_TEST_KEY = "&lt;key&gt;"
 * $env:GEMINI_LIVE_TEST_PDF = "C:\path\to\lecture-notes.pdf"
 * .\mvnw.cmd test "-Dtest=StudyPlanPromptLiveTest"
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_LIVE_TEST_KEY", matches = ".+")
class StudyPlanPromptLiveTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Course COURSE = new Course("IS442", "Object Oriented Programming");
    private static final StudyPlanProperties LIMITS =
            new StudyPlanProperties(DataSize.ofMegabytes(10), 4000, 100000, 40, 5, 5);

    private static GeminiClient client() {
        String model = System.getenv().getOrDefault("GEMINI_LIVE_TEST_MODEL", "gemini-3.5-flash");
        AiAssistantProperties properties = new AiAssistantProperties(true,
                "https://generativelanguage.googleapis.com/v1beta", model, System.getenv("GEMINI_LIVE_TEST_KEY"),
                Duration.ofSeconds(30), 4000, 2, 2, "Asia/Singapore", Duration.ofSeconds(60));
        return new GeminiClient(properties, JSON, new AiQuota(properties));
    }

    private static StudyMaterial material() throws IOException {
        String pdf = System.getenv("GEMINI_LIVE_TEST_PDF");
        if (pdf == null || pdf.isBlank()) {
            return StudyMaterial.pastedTopics("Encapsulation\nInheritance\nPolymorphism\nInterfaces\nGenerics\nCollections");
        }
        Path path = Path.of(pdf);
        return StudyMaterial.pdf(path.getFileName().toString(), Files.readAllBytes(path));
    }

    private static void plan(RoadmapFrame frame) throws IOException {
        StudyMaterial material = material();
        String prompt = new StudyPlanPromptBuilder(LIMITS).build(COURSE, StudyGoal.EXAM_PREPARATION, frame, material);

        String reply = client().generateJson(1L, prompt, material.attachments());
        Optional<RoadmapDraft> draft = new StudyPlanOutputValidator(LIMITS, JSON).validate(reply, frame);

        assertTrue(draft.isPresent(), "Rejected reply:\n" + reply);
        System.out.println("Topics: " + draft.get().sourceTopics());
        draft.get().entries().forEach(entry -> System.out.println(frame.kind().label() + " " + entry.sequence()
                + " | " + entry.plannedMinutes() + " min | " + entry.title() + " | " + entry.tasks()));
    }

    @Test
    void writesAPersonalRoadmapThatPassesTheChecks() throws IOException {
        plan(RoadmapFrame.weeks(4, 360));
    }

    @Test
    void writesAOneMeetingAgendaThatPassesTheChecks() throws IOException {
        plan(RoadmapFrame.meetings(List.of(LocalDate.now().plusDays(7)), 120));
    }
}
