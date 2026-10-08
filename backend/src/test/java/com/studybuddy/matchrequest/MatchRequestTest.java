package com.studybuddy.matchrequest;

import com.studybuddy.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.studybuddy.matchrequest.MatchRequestFixtures.pendingRequest;
import static com.studybuddy.matchrequest.MatchRequestFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchRequestTest {

    private Student alice;
    private Student bob;
    private MatchRequest request;

    @BeforeEach
    void setUp() {
        alice = student(1L, "Alice");
        bob = student(2L, "Bob");
        request = pendingRequest(10L, alice, bob);
    }

    @Test
    void newRequestIsPendingWithNoResponseTime() {
        assertTrue(request.isPending());
        assertEquals(MatchRequestStatus.PENDING, request.getStatus());
        assertNull(request.getRespondedAt());
    }

    @Test
    void acceptMovesPendingRequestToAccepted() {
        request.accept();

        assertEquals(MatchRequestStatus.ACCEPTED, request.getStatus());
        assertFalse(request.isPending());
        assertNotNull(request.getRespondedAt());
    }

    @Test
    void declineMovesPendingRequestToDeclined() {
        request.decline();

        assertEquals(MatchRequestStatus.DECLINED, request.getStatus());
        assertFalse(request.isPending());
        assertNotNull(request.getRespondedAt());
    }

    @Test
    void acceptThrowsWhenAlreadyAccepted() {
        request.accept();

        assertThrows(IllegalStateException.class, request::accept);
    }

    @Test
    void acceptThrowsWhenAlreadyDeclined() {
        request.decline();

        assertThrows(IllegalStateException.class, request::accept);
        assertEquals(MatchRequestStatus.DECLINED, request.getStatus());
    }

    @Test
    void declineThrowsWhenAlreadyAccepted() {
        request.accept();

        assertThrows(IllegalStateException.class, request::decline);
        assertEquals(MatchRequestStatus.ACCEPTED, request.getStatus());
    }

    @Test
    void declineThrowsWhenAlreadyDeclined() {
        request.decline();

        assertThrows(IllegalStateException.class, request::decline);
    }

    @Test
    void involvesSenderAndReceiverOnly() {
        assertTrue(request.involves(1L));
        assertTrue(request.involves(2L));
        assertFalse(request.involves(3L));
    }

    @Test
    void isReceiverIsTrueOnlyForReceiver() {
        assertTrue(request.isReceiver(2L));
        assertFalse(request.isReceiver(1L));
    }
}
