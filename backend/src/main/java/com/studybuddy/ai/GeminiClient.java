package com.studybuddy.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
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
 * Calls Google Gemini's generateContent endpoint for the study assistant, with any attached file
 * sent inline in the same request. A separate class from E1's {@code GeminiExplanationProvider}, so
 * the assistant can change without touching E1. Any failure is logged without the prompt, file or
 * key and thrown as {@link AiUnavailableException}.
 */
@Component
public class GeminiClient implements AiClient {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiClient.class);
    /**
     * Thinking tokens count against the output limit, and a roadmap or quiz is shaped by the
     * prompt and schema checks rather than by a reasoning step. Matches E1.
     */
    private static final int NO_THINKING = 0;
    /** Any other finish reason (the token limit, a safety block) means the JSON may be cut off. */
    private static final String FINISHED_NORMALLY = "STOP";

    private final AiAssistantProperties properties;
    private final JsonMapper json;
    private final AiQuota quota;
    private final RestClient client;
    private final boolean turnOffThinking;

    @Autowired
    public GeminiClient(AiAssistantProperties properties, JsonMapper json, AiQuota quota) {
        this(properties, json, quota, true);
    }

    /**
     * @param turnOffThinking whether to ask the model to skip its thinking step; models without one,
     *        such as the lite models, reject the setting, so it must then be left out
     */
    public GeminiClient(AiAssistantProperties properties, JsonMapper json, AiQuota quota, boolean turnOffThinking) {
        this.properties = properties;
        this.turnOffThinking = turnOffThinking;
        this.json = json;
        this.quota = quota;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requests).build();
    }

    @Override
    public String generateJson(Long studentId, String prompt, List<AiAttachment> attachments) {
        // Checked before the quota, so a switched-off assistant never spends anyone's allowance.
        if (!properties.ready()) {
            LOG.warn("Gemini assistant is switched off or has no api-key under app.ai.assistant");
            throw new AiUnavailableException();
        }
        if (!quota.tryAcquire(studentId)) {
            LOG.warn("Gemini assistant call refused: over the daily allowance or pausing after a rate limit");
            throw new AiUnavailableException();
        }
        try {
            String reply = client.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(requestFor(prompt, attachments)))
                    .retrieve()
                    .body(String.class);
            return textOf(reply).orElseThrow(AiUnavailableException::new);
        } catch (HttpClientErrorException.TooManyRequests rateLimited) {
            // Free tiers limit calls per minute; retrying at once would only fail again.
            quota.pauseAfterRateLimit();
            LOG.warn("Gemini assistant rate limited; pausing calls for {}", properties.rateLimitCooldown());
            throw new AiUnavailableException();
        } catch (RestClientException | JacksonException failure) {
            LOG.warn("Gemini assistant unavailable: {}", failure.getClass().getSimpleName());
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
        generation.put("maxOutputTokens", properties.maxOutputTokens());
        if (turnOffThinking) {
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
            LOG.warn("Gemini assistant reply unusable: finish reason {}", finishReason.isEmpty() ? "missing" : finishReason);
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
