package com.studybuddy.matchexplanation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiExplanationWriterTest {
    private static final String GOOD_REPLY = "{\"explanation\":\"You both take IS442.\"}";

    @Mock private ExplanationCache cache;
    @Mock private ExplanationQuota quota;
    @Mock private ExplanationProvider provider;

    private final ExplanationEvidence evidence = new ExplanationEvidenceFactory().from(ExplanationFixtures.evaluation());
    private AiExplanationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new AiExplanationWriter(cache, quota, new ExplanationPromptBuilder(ExplanationFixtures.JSON), provider,
                new ExplanationOutputValidator(600, ExplanationFixtures.JSON));
        when(cache.find(evidence)).thenReturn(Optional.empty());
        when(quota.tryAcquire()).thenReturn(true);
        when(provider.explain(anyString())).thenReturn(Optional.of(GOOD_REPLY));
    }

    @Test
    void cacheHitSkipsTheProvider() {
        when(cache.find(evidence)).thenReturn(Optional.of("Cached answer."));

        assertEquals(Optional.of("Cached answer."), writer.write(evidence));
        verifyNoInteractions(provider, quota);
    }

    @Test
    void aGoodAnswerIsReturnedAndCached() {
        assertEquals(Optional.of("You both take IS442."), writer.write(evidence));
        verify(cache).save(evidence, "You both take IS442.");
    }

    @Test
    void noAnswerGivesNothingAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.empty());

        assertTrue(writer.write(evidence).isEmpty());
        verify(cache, never()).save(any(), anyString());
    }

    @Test
    void aRejectedAnswerGivesNothingAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.of("{\"explanation\":\"A 90% chance of success.\"}"));

        assertTrue(writer.write(evidence).isEmpty());
        verify(cache, never()).save(any(), anyString());
    }

    @Test
    void anExhaustedQuotaGivesNothingWithoutCallingTheProvider() {
        when(quota.tryAcquire()).thenReturn(false);

        assertTrue(writer.write(evidence).isEmpty());
        verifyNoInteractions(provider);
    }

    @Test
    void cacheReadFailureGivesNothingWithoutSpendingQuota() {
        when(cache.find(evidence)).thenThrow(new DataAccessResourceFailureException("cache unavailable"));

        assertTrue(writer.write(evidence).isEmpty());
        verifyNoInteractions(quota, provider);
    }

    @Test
    void cacheWriteFailureStillReturnsTheValidatedAnswer() {
        doThrow(new DataIntegrityViolationException("cache write unavailable")).when(cache).save(evidence, "You both take IS442.");

        assertEquals(Optional.of("You both take IS442."), writer.write(evidence));
    }

    @Test
    void cacheHitsAreRevalidatedAgainstCurrentEvidenceAndLengthPolicy() {
        for (String cached : new String[] {"Study LAW999 together.", "You share 20 hours per week.", "a".repeat(601)}) {
            when(cache.find(evidence)).thenReturn(Optional.of(cached));
            assertTrue(writer.write(evidence).isEmpty(), cached);
        }
        verifyNoInteractions(quota, provider);
    }
}
