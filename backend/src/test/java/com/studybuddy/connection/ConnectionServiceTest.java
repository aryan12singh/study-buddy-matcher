package com.studybuddy.connection;

import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.studybuddy.connection.ConnectionFixtures.connection;
import static com.studybuddy.connection.ConnectionFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long CONNECTION_ID = 7L;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private NotificationService notificationService;

    private ConnectionService service;
    private Student alice;
    private Student bob;
    private Student carol;

    @BeforeEach
    void setUp() {
        service = new ConnectionService(connectionRepository, new ConnectionAssembler(), notificationService);
        alice = student(ALICE_ID, "Alice");
        bob = student(BOB_ID, "Bob");
        carol = student(CAROL_ID, "Carol");
    }

    // --- listActive ---

    @Test
    void listActiveShowsTheOtherStudentFromTheViewersSide() {
        when(connectionRepository.findActiveByStudentId(BOB_ID))
                .thenReturn(List.of(connection(CONNECTION_ID, alice, bob)));

        List<ConnectionDto> connections = service.listActive(BOB_ID);

        assertEquals(1, connections.size());
        assertEquals(CONNECTION_ID, connections.get(0).id());
        assertEquals(ALICE_ID, connections.get(0).otherStudentId());
        assertEquals("Alice", connections.get(0).otherStudentName());
    }

    @Test
    void listActiveExcludesEndedConnections() {
        Connection ended = connection(CONNECTION_ID, alice, bob);
        Connection stillActive = connection(8L, alice, carol);
        ended.end();
        // The query filters on endedAt; stand in for it with the entity's own rule.
        when(connectionRepository.findActiveByStudentId(ALICE_ID))
                .thenAnswer(call -> List.of(ended, stillActive).stream().filter(Connection::isActive).toList());

        List<ConnectionDto> connections = service.listActive(ALICE_ID);

        assertEquals(1, connections.size());
        assertEquals(CAROL_ID, connections.get(0).otherStudentId());
    }

    @Test
    void listActiveIsEmptyForStudentWithNoConnections() {
        when(connectionRepository.findActiveByStudentId(ALICE_ID)).thenReturn(List.of());

        assertTrue(service.listActive(ALICE_ID).isEmpty());
    }

    // --- end ---

    @Test
    void participantCanEndActiveConnection() {
        Connection connection = givenConnectionBetweenAliceAndBob();

        service.end(CONNECTION_ID, ALICE_ID);

        assertFalse(connection.isActive());
    }

    @Test
    void endNotifiesTheOtherStudent() {
        givenConnectionBetweenAliceAndBob();

        service.end(CONNECTION_ID, ALICE_ID);

        verify(notificationService).notify(eq(bob), eq(NotificationType.CONNECTION_ENDED), contains("Alice"));
    }

    @Test
    void nonParticipantCannotEndConnection() {
        Connection connection = givenConnectionBetweenAliceAndBob();

        assertThrows(NotConnectionParticipantException.class, () -> service.end(CONNECTION_ID, CAROL_ID));

        assertTrue(connection.isActive());
        verifyNoInteractions(notificationService);
    }

    @Test
    void endingAnAlreadyEndedConnectionThrowsAndNotifiesNobody() {
        Connection connection = givenConnectionBetweenAliceAndBob();
        connection.end();

        assertThrows(IllegalStateException.class, () -> service.end(CONNECTION_ID, BOB_ID));

        verifyNoInteractions(notificationService);
    }

    @Test
    void endingUnknownConnectionThrowsNotFound() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThrows(ConnectionNotFoundException.class, () -> service.end(CONNECTION_ID, ALICE_ID));
    }

    // --- endAll ---

    @Test
    void endAllEndsEveryActiveConnectionAndNotifiesEachOtherStudent() {
        Connection withBob = connection(CONNECTION_ID, alice, bob);
        Connection withCarol = connection(8L, carol, alice);
        when(connectionRepository.findActiveByStudentId(ALICE_ID)).thenReturn(List.of(withBob, withCarol));

        service.endAll(ALICE_ID);

        assertFalse(withBob.isActive());
        assertFalse(withCarol.isActive());
        verify(notificationService).notify(eq(bob), eq(NotificationType.CONNECTION_ENDED), contains("Alice"));
        verify(notificationService).notify(eq(carol), eq(NotificationType.CONNECTION_ENDED), contains("Alice"));
    }

    // --- areConnected ---

    @Test
    void areConnectedAsksForAnActiveConnection() {
        when(connectionRepository.existsActiveBetween(ALICE_ID, BOB_ID)).thenReturn(true);

        assertTrue(service.areConnected(ALICE_ID, BOB_ID));
    }

    @Test
    void dtoDoesNotExposeContactNumbers() {
        boolean hasContactField = Arrays.stream(ConnectionDto.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("contact"));

        assertFalse(hasContactField);
    }

    private Connection givenConnectionBetweenAliceAndBob() {
        Connection connection = connection(CONNECTION_ID, alice, bob);
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        return connection;
    }
}
