package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

import java.util.Optional;

/** Reads and writes cached explanation text by evidence hash, keeping the entity out of the service. */
@Component
public class ExplanationCache {
    private final MatchExplanationRepository repository;

    public ExplanationCache(MatchExplanationRepository repository) {
        this.repository = repository;
    }

    public Optional<String> find(String hash) {
        return repository.findByEvidenceHash(hash).map(MatchExplanation::getExplanationText);
    }

    public void save(String hash, String promptVersion, String model, String text) {
        repository.insertIfAbsent(hash, promptVersion, model, text);
    }
}
