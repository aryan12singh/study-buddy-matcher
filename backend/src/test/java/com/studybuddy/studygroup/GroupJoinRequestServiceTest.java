package com.studybuddy.studygroup;

import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.pendingJoinRequest;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupJoinRequestServiceTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long GROUP_ID = 5L;
    private static final Long OTHER_GROUP_ID = 6L;
    private static final Long REQUEST_ID = 20L;
    private static final int MAX_GROUP_SIZE = 3;

    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private NotificationService notificationService;

    private GroupJoinRequestService service;
    private Student alice;
    private Student bob;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        service = new GroupJoinRequestService(groupJoinRequestRepository, groupMembershipRepository,
                studentRepository, new StudyGroupLookup(studyGroupRepository), new GroupJoinRequestAssembler(),
                notificationService);
        alice = student(ALICE_ID, "Alice");
        bob = student(BOB_ID, "Bob");
        group = group(GROUP_ID, course(100L, "IS442"), alice, MAX_GROUP_SIZE);
    }

    // --- request ---

    @Test
    void studentCanAskToJoinAnOpenGroupWithRoomAndLeaderIsNotified() {
        givenGroupAndBobExist();
        when(groupMembershipRepository.countByStudyGroupId(GROUP_ID)).thenReturn(1L);
        when(groupJoinRequestRepository.save(any(GroupJoinRequest.class))).thenAnswer(call -> call.getArgument(0));

        GroupJoinRequestDto dto = service.request(GROUP_ID, BOB_ID, "  Can I join for the finals?  ");

        assertEquals(GROUP_ID, dto.groupId());
        assertEquals("Midterm crammers", dto.groupName());
        assertEquals(BOB_ID, dto.studentId());
        assertEquals("Bob", dto.studentName());
        assertEquals("Can I join for the finals?", dto.message());
        assertEquals(GroupJoinRequestStatus.PENDING, dto.status());
        verify(notificationService).notify(eq(alice), eq(NotificationType.GROUP_JOIN_REQUEST_RECEIVED),
                contains("Bob"));
    }

    @Test
    void requestStoresBlankMessageAsNoMessage() {
        givenGroupAndBobExist();
        when(groupJoinRequestRepository.save(any(GroupJoinRequest.class))).thenAnswer(call -> call.getArgument(0));

        GroupJoinRequestDto dto = service.request(GROUP_ID, BOB_ID, "   ");

        assertNull(dto.message());
    }

    @Test
    void requestToClosedGroupIsRejected() {
        givenGroupAndBobExist();
        group.close();

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.request(GROUP_ID, BOB_ID, null));

        verify(groupJoinRequestRepository, never()).save(any());
    }

    @Test
    void existingMemberCannotAskToJoinAgain() {
        givenGroupAndBobExist();
        when(groupMembershipRepository.existsByStudyGroupIdAndStudentId(GROUP_ID, BOB_ID)).thenReturn(true);

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.request(GROUP_ID, BOB_ID, null));

        verify(groupJoinRequestRepository, never()).save(any());
    }

    @Test
    void secondPendingRequestToSameGroupIsRejected() {
        givenGroupAndBobExist();
        when(groupJoinRequestRepository.existsByStudyGroupIdAndStudentIdAndStatus(
                GROUP_ID, BOB_ID, GroupJoinRequestStatus.PENDING)).thenReturn(true);

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.request(GROUP_ID, BOB_ID, null));

        verify(groupJoinRequestRepository, never()).save(any());
    }

    @Test
    void requestToFullGroupIsRejected() {
        givenGroupAndBobExist();
        when(groupMembershipRepository.countByStudyGroupId(GROUP_ID)).thenReturn((long) MAX_GROUP_SIZE);

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.request(GROUP_ID, BOB_ID, null));

        verify(groupJoinRequestRepository, never()).save(any());
    }

    @Test
    void requestToUnknownGroupIsRejected() {
        when(studyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(StudyGroupNotFoundException.class,
                () -> service.request(GROUP_ID, BOB_ID, null));
    }

    @Test
    void requestByUnknownStudentIsRejected() {
        givenGroupExists();
        when(studentRepository.findById(CAROL_ID)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> service.request(GROUP_ID, CAROL_ID, null));
    }

    // --- listPending ---

    @Test
    void leaderSeesPendingRequestsNewestFirst() {
        givenGroupExists();
        GroupJoinRequest newer = pendingJoinRequest(21L, group, student(CAROL_ID, "Carol"));
        GroupJoinRequest older = pendingJoinRequest(REQUEST_ID, group, bob);
        when(groupJoinRequestRepository.findByStudyGroupIdAndStatusOrderByCreatedAtDesc(
                GROUP_ID, GroupJoinRequestStatus.PENDING)).thenReturn(List.of(newer, older));

        List<GroupJoinRequestDto> pending = service.listPending(GROUP_ID, ALICE_ID);

        assertEquals(List.of(21L, REQUEST_ID), pending.stream().map(GroupJoinRequestDto::id).toList());
    }

    @Test
    void leaderWithNoPendingRequestsGetsEmptyList() {
        givenGroupExists();

        assertTrue(service.listPending(GROUP_ID, ALICE_ID).isEmpty());
    }

    @Test
    void nonLeaderCannotSeePendingRequests() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class, () -> service.listPending(GROUP_ID, BOB_ID));
    }

    // --- accept ---

    @Test
    void leaderCanAcceptAndStudentJoinsTheGroup() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        when(groupMembershipRepository.countByStudyGroupId(GROUP_ID)).thenReturn(1L);

        GroupJoinRequestDto dto = service.accept(GROUP_ID, REQUEST_ID, ALICE_ID);

        assertEquals(GroupJoinRequestStatus.ACCEPTED, dto.status());
        assertEquals(GroupJoinRequestStatus.ACCEPTED, request.getStatus());
        ArgumentCaptor<GroupMembership> membership = ArgumentCaptor.forClass(GroupMembership.class);
        verify(groupMembershipRepository).save(membership.capture());
        assertEquals(bob, membership.getValue().getStudent());
        assertEquals(group, membership.getValue().getStudyGroup());
        verify(notificationService).notify(eq(bob), eq(NotificationType.GROUP_JOIN_REQUEST_ACCEPTED),
                contains("Midterm crammers"));
    }

    @Test
    void leaderCountsTowardMaxSizeSoAFullGroupCannotAccept() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        // Alice (the leader) and two members fill a group of three.
        when(groupMembershipRepository.countByStudyGroupId(GROUP_ID)).thenReturn((long) MAX_GROUP_SIZE);

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, ALICE_ID));

        assertTrue(request.isPending());
        verify(groupMembershipRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void acceptInClosedGroupIsRejected() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        group.close();

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, ALICE_ID));

        assertTrue(request.isPending());
        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void acceptingAnAlreadyRejectedRequestIsRejected() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        request.reject();

        assertThrows(IllegalStateException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, ALICE_ID));

        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void acceptingTwiceReportsNoLongerPendingEvenOnceTheGroupIsFull() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        request.accept();

        assertThrows(IllegalStateException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, ALICE_ID));

        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void nonLeaderCannotAccept() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, BOB_ID));

        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void requestFromAnotherGroupIsTreatedAsNotFound() {
        StudyGroup otherGroup = group(OTHER_GROUP_ID, course(101L, "IS216"), alice, MAX_GROUP_SIZE);
        when(studyGroupRepository.findById(OTHER_GROUP_ID)).thenReturn(Optional.of(otherGroup));
        when(groupJoinRequestRepository.findById(REQUEST_ID))
                .thenReturn(Optional.of(pendingJoinRequest(REQUEST_ID, group, bob)));

        assertThrows(GroupJoinRequestNotFoundException.class,
                () -> service.accept(OTHER_GROUP_ID, REQUEST_ID, ALICE_ID));

        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void acceptingUnknownRequestIsRejected() {
        givenGroupExists();
        when(groupJoinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThrows(GroupJoinRequestNotFoundException.class,
                () -> service.accept(GROUP_ID, REQUEST_ID, ALICE_ID));
    }

    // --- reject ---

    @Test
    void leaderCanRejectAndStudentIsNotified() {
        GroupJoinRequest request = givenPendingRequestFromBob();

        GroupJoinRequestDto dto = service.reject(GROUP_ID, REQUEST_ID, ALICE_ID);

        assertEquals(GroupJoinRequestStatus.REJECTED, dto.status());
        assertEquals(GroupJoinRequestStatus.REJECTED, request.getStatus());
        verify(groupMembershipRepository, never()).save(any());
        verify(notificationService).notify(eq(bob), eq(NotificationType.GROUP_JOIN_REQUEST_REJECTED),
                contains("Midterm crammers"));
    }

    @Test
    void rejectingAnAlreadyAcceptedRequestIsRejected() {
        GroupJoinRequest request = givenPendingRequestFromBob();
        request.accept();

        assertThrows(IllegalStateException.class,
                () -> service.reject(GROUP_ID, REQUEST_ID, ALICE_ID));

        verifyNoInteractions(notificationService);
    }

    @Test
    void nonLeaderCannotReject() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class,
                () -> service.reject(GROUP_ID, REQUEST_ID, BOB_ID));

        verifyNoInteractions(notificationService);
    }

    // --- rejectAllPendingFrom (account deactivation) ---

    @Test
    void rejectAllPendingFromRejectsTheStudentsRequestsWithoutNotifying() {
        GroupJoinRequest request = pendingJoinRequest(REQUEST_ID, group, bob);
        when(groupJoinRequestRepository.findByStudentIdAndStatus(BOB_ID, GroupJoinRequestStatus.PENDING))
                .thenReturn(List.of(request));

        service.rejectAllPendingFrom(BOB_ID);

        assertEquals(GroupJoinRequestStatus.REJECTED, request.getStatus());
        verifyNoInteractions(notificationService);
    }

    // --- helpers ---

    private void givenGroupExists() {
        when(studyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
    }

    private void givenGroupAndBobExist() {
        givenGroupExists();
        when(studentRepository.findById(BOB_ID)).thenReturn(Optional.of(bob));
    }

    private GroupJoinRequest givenPendingRequestFromBob() {
        givenGroupExists();
        GroupJoinRequest request = pendingJoinRequest(REQUEST_ID, group, bob);
        when(groupJoinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        return request;
    }
}
