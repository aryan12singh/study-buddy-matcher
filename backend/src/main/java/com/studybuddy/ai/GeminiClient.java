package com.studybuddy.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls Google Gemini's generateContent endpoint, with any attached file sent inline in the same
 * request. One instance per feature, each with its own {@link GeminiSettings} and {@link AiQuota}:
 * the study tools, the assistant chat and E1 explanations share this code but not a key, model or
 * allowance. Any failure is logged without the prompt, file or key and thrown as
 * {@link AiUnavailableException}.
 */
public class GeminiClient implements AiClient {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiClient.class);
    /**
     * Thinking tokens count against the output limit: on gemini-3.5-flash a 200-token reply
     * stopped after 189 thinking tokens. The features here are shaped by the prompt and output
     * checks rather than by a reasoning step.
     */
    private static final int NO_THINKING = 0;
    /** Any other finish reason (the token limit, a safety block) means the JSON may be cut off. */
    private static final String FINISHED_NORMALLY = "STOP";

    private final GeminiSettings settings;
    private final JsonMapper json;
    private final AiQuota quota;
    private final RestClient client;

    public GeminiClient(GeminiSettings settings, JsonMapper json, AiQuota quota) {
        this.settings = settings;
        this.json = json;
        this.quota = quota;
        HttpClient http = HttpClient.newBuilder().connectTimeout(settings.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(settings.timeout());
        this.client = RestClient.builder().baseUrl(settings.baseUrl()).requestFactory(requests).build();
    }

    @Override
    public String generateJson(Long studentId, String prompt, List<AiAttachment> attachments) {
        // Checked before the quota, so a switched-off feature never spends anyone's allowance.
        if (!settings.ready()) {
            LOG.warn("Gemini {} is switched off or has no api-key", settings.purpose());
            throw new AiUnavailableException();
        }
        if (!quota.tryAcquire(studentId)) {
            LOG.warn("Gemini {} call refused: over the daily allowance or pausing after a rate limit", settings.purpose());
            throw new AiUnavailableException();
        }
        try {
            String reply = client.post()
                    .uri("/models/{model}:generateContent", settings.model())
                    .header("x-goog-api-key", settings.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(requestFor(prompt, attachments)))
                    .retrieve()
                    .body(String.class);
            return textOf(reply).orElseThrow(AiUnavailableException::new);
        } catch (HttpClientErrorException.TooManyRequests rateLimited) {
            // Free tiers limit calls per minute; retrying at once would only fail again.
            quota.pauseAfterRateLimit();
            LOG.warn("Gemini {} rate limited; pausing calls", settings.purpose());
            throw new AiUnavailableException();
        } catch (RestClientException | JacksonException failure) {
            LOG.warn("Gemini {} unavailable: {}", settings.purpose(), failure.getClass().getSimpleName());
            throw new AiUnavailableException();
        }
    }

    /** Files go before the instructions, as Google recommends for prompts about a document. */
    private Map<String, Object> requestFor(String prompt, List<AiAttachment> attachments) {
        List<Map<String, Object>> parts = new ArrayList<>();
        for (AiAttachment attachment : attachments) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", attachment.mediaType(),
                    "data", Base64.getEncoder().encodeToString(attachment.data()))));
        }
        parts.add(Map.of("text", prompt));
        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("responseMimeType", "application/json");
        generation.put("maxOutputTokens", settings.maxOutputTokens());
        if (settings.turnOffThinking()) {
            generation.put("thinkingConfig", Map.of("thinkingBudget", NO_THINKING));
        }
        return Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", generation);
    }

    /** The text of the first candidate, only if Gemini finished normally. Thought parts are skipped. */
    private Optional<String> textOf(String reply) {
        if (reply == null) {
            return Optional.empty();
        }
        JsonNode candidate = json.readTree(reply).path("candidates").path(0);
        String finishReason = candidate.path("finishReason").asString("");
        if (!FINISHED_NORMALLY.equals(finishReason)) {
            LOG.warn("Gemini {} reply unusable: finish reason {}", settings.purpose(),
                    finishReason.isEmpty() ? "missing" : finishReason);
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (part.path("text").isString() && !part.path("thought").asBoolean(false)) {
                text.append(part.path("text").asString());
            }
        }
        return text.isEmpty() || text.toString().isBlank() ? Optional.empty() : Optional.of(text.toString());
    }
}
