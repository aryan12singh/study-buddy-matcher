package com.studybuddy.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.studybuddy.connection.ConnectionFixtures.connection;
import static com.studybuddy.connection.ConnectionFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionTest {

    private Connection connection;

    @BeforeEach
    void setUp() {
        connection = connection(7L, student(1L, "Alice"), student(2L, "Bob"));
    }

    @Test
    void newConnectionIsActive() {
        assertTrue(connection.isActive());
        assertNull(connection.getEndedAt());
    }

    @Test
    void endMakesConnectionInactiveAndRecordsWhen() {
        connection.end();

        assertFalse(connection.isActive());
        assertNotNull(connection.getEndedAt());
    }

    @Test
    void endThrowsWhenAlreadyEnded() {
        connection.end();

        assertThrows(IllegalStateException.class, connection::end);
    }

    @Test
    void involvesIsTrueOnlyForTheTwoStudents() {
        assertTrue(connection.involves(1L));
        assertTrue(connection.involves(2L));
        assertFalse(connection.involves(3L));
    }

    @Test
    void otherStudentWorksFromEitherSide() {
        assertEquals(2L, connection.otherStudent(1L).getId());
        assertEquals(1L, connection.otherStudent(2L).getId());
    }

    @Test
    void otherStudentThrowsForNonParticipant() {
        assertThrows(IllegalArgumentException.class, () -> connection.otherStudent(3L));
    }
}
