package com.studybuddy.matchexplanation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Fills the versioned prompt template with evidence JSON. The version is part of the cache key,
 * so editing the wording means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class ExplanationPromptBuilder {
    public static final String PROMPT_VERSION = "v4";
    private static final String TEMPLATE_PATH = "prompts/match-explanation-" + PROMPT_VERSION + ".txt";
    private static final String EVIDENCE_SLOT = "{{evidence}}";

    private final JsonMapper json;
    private final String template;

    public ExplanationPromptBuilder(JsonMapper json) {
        this.json = json;
        this.template = loadTemplate();
    }

    public String build(ExplanationEvidence evidence) {
        return template.replace(EVIDENCE_SLOT, json.writeValueAsString(ExplanationVocabulary.describe(evidence)));
    }

    private static String loadTemplate() {
        try (InputStream input = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return StreamUtils.copyToString(input, StandardCharsets.UTF_8);
        } catch (IOException missing) {
            throw new IllegalStateException("Prompt template " + TEMPLATE_PATH + " is missing", missing);
        }
    }
}
