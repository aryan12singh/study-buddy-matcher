package com.studybuddy.studygroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.pendingJoinRequest;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupJoinRequestTest {

    private GroupJoinRequest request;

    @BeforeEach
    void setUp() {
        StudyGroup group = group(5L, course(1L, "IS442"), student(1L, "Alice"), 4);
        request = pendingJoinRequest(20L, group, student(2L, "Bob"));
    }

    @Test
    void newRequestIsPendingWithNoResponseTime() {
        assertTrue(request.isPending());
        assertEquals(GroupJoinRequestStatus.PENDING, request.getStatus());
        assertNull(request.getRespondedAt());
    }

    @Test
    void acceptMovesPendingRequestToAccepted() {
        request.accept();

        assertEquals(GroupJoinRequestStatus.ACCEPTED, request.getStatus());
        assertFalse(request.isPending());
        assertNotNull(request.getRespondedAt());
    }

    @Test
    void rejectMovesPendingRequestToRejected() {
        request.reject();

        assertEquals(GroupJoinRequestStatus.REJECTED, request.getStatus());
        assertFalse(request.isPending());
        assertNotNull(request.getRespondedAt());
    }

    @Test
    void acceptThrowsWhenAlreadyRejected() {
        request.reject();

        assertThrows(IllegalStateException.class, request::accept);
        assertEquals(GroupJoinRequestStatus.REJECTED, request.getStatus());
    }

    @Test
    void rejectThrowsWhenAlreadyAccepted() {
        request.accept();

        assertThrows(IllegalStateException.class, request::reject);
        assertEquals(GroupJoinRequestStatus.ACCEPTED, request.getStatus());
    }

    @Test
    void acceptThrowsWhenAlreadyAccepted() {
        request.accept();

        assertThrows(IllegalStateException.class, request::accept);
    }

    @Test
    void belongsToIsTrueOnlyForItsOwnGroup() {
        assertTrue(request.belongsTo(5L));
        assertFalse(request.belongsTo(6L));
    }
}
