package com.studybuddy.matchexplanation;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Explains one match. Scores come from the matching engine unchanged; the AI provider only
 * words them. Whenever the provider is off, over quota, failing or gives an unusable answer,
 * the matching engine's own explanation is returned instead, so this never fails because AI did.
 * Deliberately not transactional: no database connection is held while the provider is called.
 */
@Service
public class MatchExplanationService {
    private static final Logger LOG = LoggerFactory.getLogger(MatchExplanationService.class);
    private final MatchService matches;
    private final ExplanationEvidenceFactory evidenceFactory;
    private final ExplanationHasher hasher;
    private final ExplanationCache cache;
    private final ExplanationQuota quota;
    private final ExplanationPromptBuilder prompts;
    private final ExplanationProvider provider;
    private final ExplanationOutputValidator validator;
    private final MatchExplanationAssembler assembler;
    private final AiExplanationProperties properties;

    public MatchExplanationService(MatchService matches, ExplanationEvidenceFactory evidenceFactory,
            ExplanationHasher hasher, ExplanationCache cache, ExplanationQuota quota, ExplanationPromptBuilder prompts,
            ExplanationProvider provider, ExplanationOutputValidator validator, MatchExplanationAssembler assembler,
            AiExplanationProperties properties) {
        this.matches = matches;
        this.evidenceFactory = evidenceFactory;
        this.hasher = hasher;
        this.cache = cache;
        this.quota = quota;
        this.prompts = prompts;
        this.provider = provider;
        this.validator = validator;
        this.assembler = assembler;
        this.properties = properties;
    }

    public MatchExplanationDto explain(MatchSearchCriteria search, Long actorId, Long candidateId, String evaluationId) {
        MatchEvaluation evaluation = matches.evaluate(search, actorId, candidateId);
        requireCurrentEvaluation(evaluationId, evaluation.result().evaluationId());
        String standardText = evaluation.result().explanation();
        if (!properties.ready()) {
            return assembler.standard(candidateId, evaluationId, standardText);
        }
        ExplanationEvidence evidence = evidenceFactory.from(evaluation);
        String hash = hasher.hash(evidence, ExplanationPromptBuilder.PROMPT_VERSION, properties.model());
        Optional<String> cached;
        try {
            cached = cache.find(hash);
        } catch (DataAccessException failure) {
            logCacheFailure("read", failure);
            return assembler.standard(candidateId, evaluationId, standardText);
        }
        if (cached.isPresent()) {
            // Apply today's evidence and length policy even to previously accepted text.
            return validator.validateText(cached.get(), evidence)
                    .map(text -> assembler.ai(candidateId, evaluationId, text))
                    .orElseGet(() -> assembler.standard(candidateId, evaluationId, standardText));
        }
        if (!quota.tryAcquire()) {
            return assembler.standard(candidateId, evaluationId, standardText);
        }
        Optional<String> written = provider.explain(prompts.build(evidence)).flatMap(raw -> validator.validate(raw, evidence));
        if (written.isEmpty()) {
            return assembler.standard(candidateId, evaluationId, standardText);
        }
        try {
            cache.save(hash, ExplanationPromptBuilder.PROMPT_VERSION, properties.model(), written.get());
        } catch (DataAccessException failure) {
            logCacheFailure("write", failure);
        }
        return assembler.ai(candidateId, evaluationId, written.get());
    }

    private static void requireCurrentEvaluation(String requested, String current) {
        if (requested == null || !requested.matches("[0-9a-f]{64}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "A valid card evaluation ID is required");
        }
        if (!requested.equals(current)) {
            throw new StaleMatchEvaluationException();
        }
    }

    private static void logCacheFailure(String operation, DataAccessException failure) {
        // Exception messages can contain SQL and data. Record only the operation and class.
        LOG.warn("Optional explanation cache {} failed: {}", operation, failure.getClass().getSimpleName());
    }
}
