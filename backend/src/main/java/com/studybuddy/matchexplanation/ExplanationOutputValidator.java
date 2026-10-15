package com.studybuddy.matchexplanation;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides whether the provider's reply is safe to show. Anything malformed, too long or claiming
 * a likelihood of success is rejected, and the caller falls back to the standard explanation.
 */
public class ExplanationOutputValidator {
    /** Phrases that would present a compatibility score as a prediction. */
    private static final List<String> BANNED_PHRASES =
            List.of("probability", "chance of success", "guarantee", "likely to succeed", "%");

    private final int maxCharacters;
    private final JsonMapper json;

    public ExplanationOutputValidator(int maxCharacters, JsonMapper json) {
        this.maxCharacters = maxCharacters;
        this.json = json;
    }

    public Optional<String> validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        JsonNode reply;
        try {
            reply = json.readTree(raw);
        } catch (JacksonException notJson) {
            return Optional.empty();
        }
        JsonNode explanation = reply.path("explanation");
        if (!explanation.isString()) {
            return Optional.empty();
        }
        String text = explanation.asString().strip();
        if (text.isEmpty() || text.length() > maxCharacters || makesSuccessClaim(text)) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private static boolean makesSuccessClaim(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return BANNED_PHRASES.stream().anyMatch(lower::contains);
    }
}
