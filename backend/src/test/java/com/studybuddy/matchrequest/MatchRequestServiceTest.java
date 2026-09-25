package com.studybuddy.matchrequest;

import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.studybuddy.matchrequest.MatchRequestFixtures.pendingRequest;
import static com.studybuddy.matchrequest.MatchRequestFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchRequestServiceTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long REQUEST_ID = 10L;

    @Mock
    private MatchRequestRepository matchRequestRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private StudentRepository studentRepository;

    private MatchRequestService service;
    private Student alice;
    private Student bob;

    @BeforeEach
    void setUp() {
        service = new MatchRequestService(matchRequestRepository, connectionRepository,
                studentRepository, new MatchRequestAssembler());
        alice = student(ALICE_ID, "Alice");
        bob = student(BOB_ID, "Bob");
    }

    // --- send ---

    @Test
    void sendSavesPendingRequestAndReturnsDto() {
        givenStudentsExist();
        when(matchRequestRepository.save(any(MatchRequest.class))).thenAnswer(call -> call.getArgument(0));

        MatchRequestDto dto = service.send(ALICE_ID, BOB_ID, "  Want to revise for the midterm?  ");

        assertEquals(ALICE_ID, dto.senderId());
        assertEquals("Alice", dto.senderName());
        assertEquals(BOB_ID, dto.receiverId());
        assertEquals("Bob", dto.receiverName());
        assertEquals("Want to revise for the midterm?", dto.message());
        assertEquals(MatchRequestStatus.PENDING, dto.status());
    }

    @Test
    void sendStoresBlankMessageAsNoMessage() {
        givenStudentsExist();
        when(matchRequestRepository.save(any(MatchRequest.class))).thenAnswer(call -> call.getArgument(0));

        MatchRequestDto dto = service.send(ALICE_ID, BOB_ID, "   ");

        assertNull(dto.message());
    }

    @Test
    void sendToSelfIsRejected() {
        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.send(ALICE_ID, ALICE_ID, null));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void sendToAlreadyConnectedStudentIsRejected() {
        givenStudentsExist();
        when(connectionRepository.existsActiveBetween(ALICE_ID, BOB_ID)).thenReturn(true);

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.send(ALICE_ID, BOB_ID, null));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void sendWhilePendingRequestExistsIsRejected() {
        givenStudentsExist();
        when(matchRequestRepository.existsPendingBetween(ALICE_ID, BOB_ID)).thenReturn(true);

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.send(ALICE_ID, BOB_ID, null));

        verify(matchRequestRepository, never()).save(any());
    }

    @Test
    void sendToUnknownStudentIsRejected() {
        when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.of(alice));
        when(studentRepository.findById(CAROL_ID)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> service.send(ALICE_ID, CAROL_ID, null));

        verify(matchRequestRepository, never()).save(any());
    }

    // --- accept ---

    @Test
    void receiverCanAcceptPendingRequest() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        MatchRequestDto dto = service.accept(REQUEST_ID, BOB_ID);

        assertEquals(MatchRequestStatus.ACCEPTED, dto.status());
        assertEquals(MatchRequestStatus.ACCEPTED, request.getStatus());
    }

    @Test
    void acceptCreatesExactlyOneConnectionBetweenTheTwoStudents() {
        givenPendingRequestFromAliceToBob();

        service.accept(REQUEST_ID, BOB_ID);

        ArgumentCaptor<Connection> saved = ArgumentCaptor.forClass(Connection.class);
        verify(connectionRepository, times(1)).save(saved.capture());
        assertSame(alice, saved.getValue().getStudentA());
        assertSame(bob, saved.getValue().getStudentB());
    }

    @Test
    void senderCannotAcceptTheirOwnRequest() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.accept(REQUEST_ID, ALICE_ID));

        assertTrue(request.isPending());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void uninvolvedStudentCannotAccept() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.accept(REQUEST_ID, CAROL_ID));

        assertTrue(request.isPending());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void acceptingNonPendingRequestThrowsAndCreatesNoConnection() {
        MatchRequest request = givenPendingRequestFromAliceToBob();
        request.decline();

        assertThrows(IllegalStateException.class, () -> service.accept(REQUEST_ID, BOB_ID));

        verify(connectionRepository, never()).save(any());
    }

    @Test
    void acceptingUnknownRequestThrowsNotFound() {
        when(matchRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(MatchRequestNotFoundException.class, () -> service.accept(REQUEST_ID, BOB_ID));
    }

    // --- decline ---

    @Test
    void receiverCanDeclinePendingRequestWithoutCreatingConnection() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        MatchRequestDto dto = service.decline(REQUEST_ID, BOB_ID);

        assertEquals(MatchRequestStatus.DECLINED, dto.status());
        assertEquals(MatchRequestStatus.DECLINED, request.getStatus());
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void senderCannotDeclineTheirOwnRequest() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.decline(REQUEST_ID, ALICE_ID));

        assertTrue(request.isPending());
    }

    @Test
    void uninvolvedStudentCannotDecline() {
        MatchRequest request = givenPendingRequestFromAliceToBob();

        assertThrows(MatchRequestNotAllowedException.class,
                () -> service.decline(REQUEST_ID, CAROL_ID));

        assertTrue(request.isPending());
    }

    @Test
    void decliningNonPendingRequestThrows() {
        MatchRequest request = givenPendingRequestFromAliceToBob();
        request.accept();

        assertThrows(IllegalStateException.class, () -> service.decline(REQUEST_ID, BOB_ID));
        assertEquals(MatchRequestStatus.ACCEPTED, request.getStatus());
    }

    // --- listing ---

    @Test
    void listIncomingReturnsRequestsReceivedByStudent() {
        when(matchRequestRepository.findByReceiverIdOrderByCreatedAtDesc(BOB_ID))
                .thenReturn(List.of(pendingRequest(REQUEST_ID, alice, bob)));

        List<MatchRequestDto> incoming = service.listIncoming(BOB_ID);

        assertEquals(1, incoming.size());
        assertEquals(ALICE_ID, incoming.get(0).senderId());
    }

    @Test
    void listOutgoingReturnsEmptyListWhenStudentHasSentNothing() {
        when(matchRequestRepository.findBySenderIdOrderByCreatedAtDesc(ALICE_ID)).thenReturn(List.of());

        assertTrue(service.listOutgoing(ALICE_ID).isEmpty());
    }

    @Test
    void dtoDoesNotExposeContactNumbers() {
        boolean hasContactField = Arrays.stream(MatchRequestDto.class.getRecordComponents())
                .anyMatch(component -> component.getName().toLowerCase().contains("contact"));

        assertFalse(hasContactField);
    }

    private void givenStudentsExist() {
        when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.of(alice));
        when(studentRepository.findById(BOB_ID)).thenReturn(Optional.of(bob));
    }

    private MatchRequest givenPendingRequestFromAliceToBob() {
        MatchRequest request = pendingRequest(REQUEST_ID, alice, bob);
        when(matchRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        return request;
    }
}
