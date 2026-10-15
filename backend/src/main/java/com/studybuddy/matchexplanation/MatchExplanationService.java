package com.studybuddy.matchexplanation;

import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
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

    public MatchExplanationDto explain(MatchSearchCriteria search, Long actorId, Long candidateId) {
        MatchEvaluation evaluation = matches.evaluate(search, actorId, candidateId);
        String standardText = evaluation.result().explanation();
        if (!properties.ready()) {
            return assembler.standard(candidateId, standardText);
        }
        ExplanationEvidence evidence = evidenceFactory.from(evaluation);
        String hash = hasher.hash(evidence, ExplanationPromptBuilder.PROMPT_VERSION, properties.model());
        Optional<String> cached = cache.find(hash);
        if (cached.isPresent()) {
            return assembler.ai(candidateId, cached.get());
        }
        if (!quota.tryAcquire()) {
            return assembler.standard(candidateId, standardText);
        }
        Optional<String> written = provider.explain(prompts.build(evidence)).flatMap(validator::validate);
        if (written.isEmpty()) {
            return assembler.standard(candidateId, standardText);
        }
        cache.save(hash, ExplanationPromptBuilder.PROMPT_VERSION, properties.model(), written.get());
        return assembler.ai(candidateId, written.get());
    }
}
