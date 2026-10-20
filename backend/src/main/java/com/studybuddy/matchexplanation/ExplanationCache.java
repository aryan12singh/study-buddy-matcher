package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Stores AI wording by what it explains: a hash of the evidence, the prompt version and the model.
 * Changing any of them gives a new key, so old wording is simply never read again. The entity
 * stays inside this class; callers see only text.
 */
@Component
public class ExplanationCache {
    private final MatchExplanationRepository repository;
    private final ExplanationHasher hasher;
    private final AiExplanationProperties properties;

    public ExplanationCache(MatchExplanationRepository repository, ExplanationHasher hasher,
            AiExplanationProperties properties) {
        this.repository = repository;
        this.hasher = hasher;
        this.properties = properties;
    }

    public Optional<String> find(ExplanationEvidence evidence) {
        return repository.findByEvidenceHash(keyOf(evidence)).map(MatchExplanation::getExplanationText);
    }

    public void save(ExplanationEvidence evidence, String text) {
        repository.insertIfAbsent(keyOf(evidence), ExplanationPromptBuilder.PROMPT_VERSION, properties.model(), text);
    }

    private String keyOf(ExplanationEvidence evidence) {
        return hasher.hash(evidence, ExplanationPromptBuilder.PROMPT_VERSION, properties.model());
    }
}
