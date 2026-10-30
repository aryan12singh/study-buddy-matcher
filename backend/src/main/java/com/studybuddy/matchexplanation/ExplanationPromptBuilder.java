package com.studybuddy.matchexplanation;

import com.studybuddy.ai.PromptTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Fills the versioned prompt template with evidence JSON. The version is part of the cache key,
 * so editing the wording means adding a new template file and bumping {@link #PROMPT_VERSION}.
 */
@Component
public class ExplanationPromptBuilder {
    public static final String PROMPT_VERSION = "v5";
    private static final String TEMPLATE_PATH = "prompts/match-explanation-" + PROMPT_VERSION + ".txt";

    private final JsonMapper json;
    private final PromptTemplate template;

    public ExplanationPromptBuilder(JsonMapper json) {
        this.json = json;
        this.template = PromptTemplate.load(TEMPLATE_PATH);
    }

    public String build(ExplanationEvidence evidence) {
        return template.fill(Map.of("evidence", json.writeValueAsString(ExplanationVocabulary.describe(evidence))));
    }
}
