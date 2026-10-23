package com.studybuddy.matchexplanation;

import com.studybuddy.common.error.ApiException;
import com.studybuddy.matching.MatchSearchCriteria;
import com.studybuddy.matching.MatchService;
import com.studybuddy.student.StudentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchExplanationServiceTest {
    private static final long ME = 1L;
    private static final long PEER = ExplanationFixtures.CANDIDATE_ID;
    private static final String EVALUATION_ID = ExplanationFixtures.EVALUATION_ID;
    private static final MatchSearchCriteria SEARCH = new MatchSearchCriteria(4L, null, null, null, null, null);
    private static final String STANDARD_TEXT = "good match: same course";

    @Mock private MatchService matches;
    @Mock private AiExplanationWriter writer;

    private MatchExplanationService service(boolean ready) {
        AiExplanationProperties properties = new AiExplanationProperties(ready, "http://localhost", "test-model",
                ready ? "key" : null, Duration.ofSeconds(1), 200, 600, 10, 10, "UTC", Duration.ofSeconds(60));
        return new MatchExplanationService(matches, new ExplanationEvidenceFactory(), writer,
                new MatchExplanationAssembler(), properties);
    }

    @BeforeEach
    void setUp() {
        when(matches.evaluate(SEARCH, ME, PEER)).thenReturn(ExplanationFixtures.evaluation());
        when(writer.write(any(), eq(ME))).thenReturn(Optional.of("You both take IS442."));
    }

    @Test
    void disabledFeatureReturnsTheStandardTextWithoutAskingForAiWording() {
        MatchExplanationDto result = service(false).explain(SEARCH, ME, PEER, EVALUATION_ID);

        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, STANDARD_TEXT, ExplanationSource.STANDARD), result);
        verifyNoInteractions(writer);
    }

    @Test
    void aiWordingIsReturnedLabelledAsAi() {
        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, "You both take IS442.", ExplanationSource.AI),
                service(true).explain(SEARCH, ME, PEER, EVALUATION_ID));
    }

    @Test
    void noAiWordingFallsBackToTheStandardText() {
        when(writer.write(any(), eq(ME))).thenReturn(Optional.empty());

        assertEquals(new MatchExplanationDto(PEER, EVALUATION_ID, STANDARD_TEXT, ExplanationSource.STANDARD),
                service(true).explain(SEARCH, ME, PEER, EVALUATION_ID));
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
        verifyNoInteractions(writer);
    }

    @Test
    void missingOrMalformedCardIdentityIsRejected() {
        for (String id : new String[] {null, "", "not-a-fingerprint"}) {
            ApiException error = assertThrows(ApiException.class, () -> service(true).explain(SEARCH, ME, PEER, id));
            assertEquals("INVALID_INPUT", error.getCode());
        }
        verifyNoInteractions(writer);
    }

    @Test
    void coreMatchingDatabaseErrorsStillPassThrough() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("matching unavailable");
        when(matches.evaluate(SEARCH, ME, PEER)).thenThrow(failure);

        assertSame(failure, assertThrows(DataAccessResourceFailureException.class,
                () -> service(true).explain(SEARCH, ME, PEER, EVALUATION_ID)));
        verifyNoInteractions(writer);
    }
}
