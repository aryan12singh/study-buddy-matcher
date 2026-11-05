package com.studybuddy.matchexplanation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Gets AI wording for anonymous match evidence: from the cache when the same evidence was
 * explained before, otherwise from the provider, which spends the student's call allowance. Every
 * answer, cached or new, must pass the output checks. Empty means "use the standard explanation";
 * it never throws.
 */
@Component
public class AiExplanationWriter {
    private static final Logger LOG = LoggerFactory.getLogger(AiExplanationWriter.class);

    private final ExplanationCache cache;
    private final ExplanationPromptBuilder prompts;
    private final ExplanationProvider provider;
    private final ExplanationOutputValidator validator;

    public AiExplanationWriter(ExplanationCache cache, ExplanationPromptBuilder prompts,
            ExplanationProvider provider, ExplanationOutputValidator validator) {
        this.cache = cache;
        this.prompts = prompts;
        this.provider = provider;
        this.validator = validator;
    }

    /** The student is used only for their share of the call allowance; it never reaches the AI. */
    public Optional<String> write(ExplanationEvidence evidence, Long studentId) {
        Optional<String> cached;
        try {
            cached = cache.find(evidence);
        } catch (DataAccessException failure) {
            // Without the cache we cannot tell whether a call is needed; do not spend allowance guessing.
            logCacheFailure("read", failure);
            return Optional.empty();
        }
        if (cached.isPresent()) {
            // Today's checks apply to previously accepted text too.
            return validator.validateText(cached.get(), evidence);
        }
        Optional<String> written = provider.explain(studentId, prompts.build(evidence)).flatMap(raw -> validator.validate(raw, evidence));
        written.ifPresent(text -> saveQuietly(evidence, text));
        return written;
    }

    /** A failed save only loses a future cache hit; the student still gets this answer. */
    private void saveQuietly(ExplanationEvidence evidence, String text) {
        try {
            cache.save(evidence, text);
        } catch (DataAccessException failure) {
            logCacheFailure("write", failure);
        }
    }

    private static void logCacheFailure(String operation, DataAccessException failure) {
        // Exception messages can contain SQL and data. Record only the operation and class.
        LOG.warn("Optional explanation cache {} failed: {}", operation, failure.getClass().getSimpleName());
    }
}
