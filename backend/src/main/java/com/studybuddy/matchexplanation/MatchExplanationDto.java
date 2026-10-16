package com.studybuddy.matchexplanation;

/** An explanation of one match, safe to send to the browser. */
public record MatchExplanationDto(Long candidateId, String text, ExplanationSource source) {
}
