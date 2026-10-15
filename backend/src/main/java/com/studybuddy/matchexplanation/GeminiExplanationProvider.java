package com.studybuddy.matchexplanation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls Google Gemini's generateContent endpoint. Any failure (timeout, HTTP error, blocked or
 * unreadable reply) is logged without the prompt or key and returned as empty.
 */
@Component
public class GeminiExplanationProvider implements ExplanationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiExplanationProvider.class);

    private final AiExplanationProperties properties;
    private final JsonMapper json;
    private final RestClient client;

    public GeminiExplanationProvider(AiExplanationProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(requests).build();
    }

    @Override
    public Optional<String> explain(String prompt) {
        try {
            String reply = client.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(requestFor(prompt)))
                    .retrieve()
                    .body(String.class);
            return textOf(reply);
        } catch (RestClientException | JacksonException failure) {
            LOG.warn("Gemini explanation unavailable: {}", failure.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Map<String, Object> requestFor(String prompt) {
        return Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", properties.maxOutputTokens()));
    }

    private Optional<String> textOf(String reply) {
        if (reply == null) {
            return Optional.empty();
        }
        JsonNode text = json.readTree(reply).path("candidates").path(0).path("content").path("parts").path(0).path("text");
        return text.isString() && !text.asString().isBlank() ? Optional.of(text.asString()) : Optional.empty();
    }
}
