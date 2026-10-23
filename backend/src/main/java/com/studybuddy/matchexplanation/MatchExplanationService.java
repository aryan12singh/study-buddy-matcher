package com.studybuddy.matchexplanation;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.matching.MatchEvaluation;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
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
    private final MatchService matches;
    private final ExplanationEvidenceFactory evidenceFactory;
    private final AiExplanationWriter writer;
    private final MatchExplanationAssembler assembler;
    private final AiExplanationProperties properties;

    public MatchExplanationService(MatchService matches, ExplanationEvidenceFactory evidenceFactory,
            AiExplanationWriter writer, MatchExplanationAssembler assembler, AiExplanationProperties properties) {
        this.matches = matches;
        this.evidenceFactory = evidenceFactory;
        this.writer = writer;
        this.assembler = assembler;
        this.properties = properties;
    }

    /**
     * Re-scores the clicked card, refuses a card whose score has since changed, then returns AI
     * wording when available and the matching engine's own explanation otherwise.
     */
    public MatchExplanationDto explain(MatchSearchCriteria search, Long actorId, Long candidateId, String evaluationId) {
        MatchEvaluation evaluation = matches.evaluate(search, actorId, candidateId);
        requireCurrentEvaluation(evaluationId, evaluation.result().evaluationId());
        String standardText = evaluation.result().explanation();
        Optional<String> aiText = properties.ready()
                ? writer.write(evidenceFactory.from(evaluation), actorId)
                : Optional.empty();
        return aiText.map(text -> assembler.ai(candidateId, evaluationId, text))
                .orElseGet(() -> assembler.standard(candidateId, evaluationId, standardText));
    }

    private static void requireCurrentEvaluation(String requested, String current) {
        if (requested == null || !requested.matches("[0-9a-f]{64}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "A valid card evaluation ID is required");
        }
        if (!requested.equals(current)) {
            throw new StaleMatchEvaluationException();
        }
    }
}
