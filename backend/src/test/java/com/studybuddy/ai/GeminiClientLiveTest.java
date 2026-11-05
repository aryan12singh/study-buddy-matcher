package com.studybuddy.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that real Gemini accepts the client's requests, which the stub server in
 * {@link GeminiClientTest} cannot prove. Skipped unless {@code GEMINI_LIVE_TEST_KEY} is set, so CI
 * and everyone without a key never call Google. Each run makes two calls from that key's free
 * allowance. Run by hand and record the date and result in the pull request:
 * <pre>
 * $env:GEMINI_LIVE_TEST_KEY = "&lt;key&gt;"
 * .\mvnw.cmd test "-Dtest=GeminiClientLiveTest"
 * </pre>
 * {@code GEMINI_LIVE_TEST_MODEL} overrides the model, which otherwise matches application.yml.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_LIVE_TEST_KEY", matches = ".+")
class GeminiClientLiveTest {
    private static final Long STUDENT_ID = 1L;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static GeminiClient client() {
        String model = System.getenv().getOrDefault("GEMINI_LIVE_TEST_MODEL", "gemini-3.5-flash");
        AiAssistantProperties properties = new AiAssistantProperties(true,
                "https://generativelanguage.googleapis.com/v1beta", model, System.getenv("GEMINI_LIVE_TEST_KEY"),
                Duration.ofSeconds(30), 4000, 2, 2, "Asia/Singapore", Duration.ofSeconds(60));
        return new GeminiClient(properties.geminiSettings("live test", true), JSON, properties.newQuota());
    }

    @Test
    void answersAPlainPromptWithJson() {
        String reply = client().generateJson(STUDENT_ID,
                "Reply with exactly this JSON object and nothing else: {\"ok\": true}");

        assertTrue(JSON.readTree(reply).path("ok").asBoolean(false), reply);
    }

    @Test
    void readsAnAttachedPdf() {
        byte[] notes = onePagePdf("IS442 week 3 notes: Encapsulation, Inheritance and Polymorphism");

        String reply = client().generateJson(STUDENT_ID,
                "List the study topics named in the attached notes. Reply only with JSON shaped like "
                        + "{\"topics\": [\"...\"]}.",
                List.of(new AiAttachment("application/pdf", notes)));

        JsonNode topics = JSON.readTree(reply).path("topics");
        assertTrue(topics.isArray() && !topics.isEmpty(), reply);
        assertTrue(reply.toLowerCase(Locale.ROOT).contains("polymorphism"), reply);
    }

    /** A minimal valid PDF holding one line of text, so the test needs no file on disk. */
    private static byte[] onePagePdf(String line) {
        String stream = "BT /F1 14 Tf 72 720 Td (" + line + ") Tj ET";
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(pdf, "%PDF-1.4\n");
        for (int number = 1; number <= objects.size(); number++) {
            offsets.add(pdf.size());
            write(pdf, number + " 0 obj\n" + objects.get(number - 1) + "\nendobj\n");
        }
        int xref = pdf.size();
        write(pdf, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int offset : offsets) {
            write(pdf, String.format("%010d 00000 n \n", offset));
        }
        write(pdf, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return pdf.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.US_ASCII));
    }
}
