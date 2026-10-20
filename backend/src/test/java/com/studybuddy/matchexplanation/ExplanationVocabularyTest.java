package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplanationVocabularyTest {

    /**
     * The prompt view is written by hand, so a field added to the evidence record could be hashed
     * but never sent. Strategy is the one field deliberately kept out of the prompt.
     */
    @Test
    void describesEveryEvidenceFieldExceptTheAdminStrategy() {
        List<String> evidenceFields = Arrays.stream(ExplanationEvidence.class.getRecordComponents())
                .map(RecordComponent::getName).filter(name -> !name.equals("strategy")).toList();

        var described = ExplanationVocabulary.describe(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));

        assertEquals(evidenceFields, List.copyOf(described.keySet()));
    }

    @Test
    void describesEveryCriterionFieldInPlainWords() {
        List<String> criterionFields = Arrays.stream(CriterionEvidence.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        var described = ExplanationVocabulary.describe(new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation()));
        @SuppressWarnings("unchecked")
        var first = ((List<java.util.Map<String, Object>>) described.get("criteria")).getFirst();

        assertEquals(Set.copyOf(criterionFields), first.keySet());
    }
}
