package com.studybuddy.matchexplanation;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import com.studybuddy.student.StudentNotFoundException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchExplanationServiceTest {
    private static final long ME = 1L;
    private static final long PEER = ExplanationFixtures.CANDIDATE_ID;
    private static final String EVALUATION_ID = ExplanationFixtures.EVALUATION_ID;
    private static final MatchSearchCriteria SEARCH = new MatchSearchCriteria(4L, null, null, null, null, null);
    private static final String GOOD_REPLY = "{\"explanation\":\"You both take IS442.\"}";

    @Mock private MatchService matches;
    @Mock private ExplanationCache cache;
    @Mock private ExplanationQuota quota;
    @Mock private ExplanationProvider provider;

    private final ExplanationHasher hasher = new ExplanationHasher(ExplanationFixtures.JSON);
    private final ExplanationEvidenceFactory evidence = new ExplanationEvidenceFactory();
    private String hash;

    private MatchExplanationService service(boolean ready) {
        AiExplanationProperties properties = new AiExplanationProperties(ready, "http://localhost", "test-model",
                ready ? "key" : null, Duration.ofSeconds(1), 200, 600, 10, "UTC", Duration.ofSeconds(60));
        return new MatchExplanationService(matches, evidence, hasher, cache, quota,
                new ExplanationPromptBuilder(ExplanationFixtures.JSON), provider,
                new ExplanationOutputValidator(600, ExplanationFixtures.JSON), new MatchExplanationAssembler(), properties);
    }

    @BeforeEach
    void setUp() {
        when(matches.evaluate(SEARCH, ME, PEER)).thenReturn(ExplanationFixtures.evaluation());
        hash = hasher.hash(evidence.from(ExplanationFixtures.evaluation()), ExplanationPromptBuilder.PROMPT_VERSION, "test-model");
        when(cache.find(hash)).thenReturn(Optional.empty());
        when(quota.tryAcquire()).thenReturn(true);
        when(provider.explain(anyString())).thenReturn(Optional.of(GOOD_REPLY));
    }

    @Test
    void disabledFeatureReturnsTheStandardTextWithoutCallingAnything() {
        MatchExplanationDto result = service(false).explain(SEARCH, ME, PEER, EVALUATION_ID);

        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, "good match: same course", ExplanationSource.STANDARD), result);
        verifyNoInteractions(cache, quota, provider);
    }

    @Test
    void cacheHitSkipsTheProvider() {
        when(cache.find(hash)).thenReturn(Optional.of("Cached answer."));

        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, "Cached answer.", ExplanationSource.AI), service(true).explain(SEARCH, ME, PEER, EVALUATION_ID));
        verifyNoInteractions(provider, quota);
    }

    @Test
    void aGoodAnswerIsReturnedAndCached() {
        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, "You both take IS442.", ExplanationSource.AI), service(true).explain(SEARCH, ME, PEER, EVALUATION_ID));
        verify(cache).save(hash, ExplanationPromptBuilder.PROMPT_VERSION, "test-model", "You both take IS442.");
    }

    @Test
    void noAnswerFallsBackAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.empty());

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER, EVALUATION_ID).source());
        verify(cache, never()).save(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void aRejectedAnswerFallsBackAndCachesNothing() {
        when(provider.explain(anyString())).thenReturn(Optional.of("{\"explanation\":\"A 90% chance of success.\"}"));

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER, EVALUATION_ID).source());
        verify(cache, never()).save(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void anExhaustedQuotaFallsBackWithoutCallingTheProvider() {
        when(quota.tryAcquire()).thenReturn(false);

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER, EVALUATION_ID).source());
        verifyNoInteractions(provider);
    }

    @Test
    void searchErrorsPassThrough() {
        when(matches.evaluate(SEARCH, ME, 9L)).thenThrow(new StudentNotFoundException(9L));

        assertThrows(StudentNotFoundException.class, () -> service(true).explain(SEARCH, ME, 9L, EVALUATION_ID));
    }
    @Test
    void staleCardIsRejectedBeforeAnyOptionalWorkEvenWhenAiIsOff() {
        for (boolean ready : new boolean[] {false, true}) {
            StaleMatchEvaluationException error = assertThrows(StaleMatchEvaluationException.class,
                    () -> service(ready).explain(SEARCH, ME, PEER, "b".repeat(64)));
            assertEquals("STALE_MATCH_EVALUATION", error.getCode());
        }
        verifyNoInteractions(cache, quota, provider);
    }

    @Test
    void missingOrMalformedCardIdentityIsRejected() {
        for (String id : new String[] {null, "", "not-a-fingerprint"}) {
            ApiException error = assertThrows(ApiException.class, () -> service(true).explain(SEARCH, ME, PEER, id));
            assertEquals("INVALID_INPUT", error.getCode());
        }
        verifyNoInteractions(cache, quota, provider);
    }

    @Test
    void cacheReadFailureReturnsStandardWithoutSpendingQuota() {
        when(cache.find(hash)).thenThrow(new DataAccessResourceFailureException("cache unavailable"));

        assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER, EVALUATION_ID).source());
        verifyNoInteractions(quota, provider);
    }

    @Test
    void cacheWriteFailureStillReturnsTheValidatedAnswer() {
        doThrow(new DataIntegrityViolationException("cache write unavailable"))
                .when(cache).save(hash, ExplanationPromptBuilder.PROMPT_VERSION, "test-model", "You both take IS442.");

        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, "You both take IS442.", ExplanationSource.AI),
                service(true).explain(SEARCH, ME, PEER, EVALUATION_ID));
    }

    @Test
    void cacheHitsAreRevalidatedAgainstCurrentEvidenceAndLengthPolicy() {
        for (String cached : new String[] {"Study LAW999 together.", "You share 20 hours per week.", "a".repeat(601)}) {
            when(cache.find(hash)).thenReturn(Optional.of(cached));
            assertEquals(ExplanationSource.STANDARD, service(true).explain(SEARCH, ME, PEER, EVALUATION_ID).source());
        }
        verifyNoInteractions(quota, provider);
    }

    @Test
    void coreMatchingDatabaseErrorsStillPassThrough() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("matching unavailable");
        when(matches.evaluate(SEARCH, ME, PEER)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service(true).explain(SEARCH, ME, PEER, EVALUATION_ID)));
        verifyNoInteractions(cache, quota, provider);
    }
}
