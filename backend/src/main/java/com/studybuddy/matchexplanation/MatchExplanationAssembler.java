package com.studybuddy.matchexplanation;

import org.springframework.stereotype.Component;

/** Builds explanation responses, labelling each with where its text came from. */
@Component
public class MatchExplanationAssembler {
    public MatchExplanationDto ai(Long candidateId, String text) {
        return new MatchExplanationDto(candidateId, text, ExplanationSource.AI);
    }

    public MatchExplanationDto standard(Long candidateId, String text) {
        return new MatchExplanationDto(candidateId, text, ExplanationSource.STANDARD);
    }
}
