package com.studybuddy.studygroup;

import com.studybuddy.course.Course;
import com.studybuddy.course.CourseNotFoundException;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentNotFoundException;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.student.StudyGoal;
import com.studybuddy.student.StudyMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.studybuddy.studygroup.StudyGroupFixtures.course;
import static com.studybuddy.studygroup.StudyGroupFixtures.group;
import static com.studybuddy.studygroup.StudyGroupFixtures.membership;
import static com.studybuddy.studygroup.StudyGroupFixtures.pendingJoinRequest;
import static com.studybuddy.studygroup.StudyGroupFixtures.student;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyGroupServiceTest {

    private static final Long ALICE_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long IS442_ID = 100L;
    private static final Long IS216_ID = 101L;
    private static final Long GROUP_ID = 5L;

    private static final GroupAvailabilitySlotDto MONDAY_EVENING =
            new GroupAvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private GroupAvailabilitySlotRepository groupAvailabilitySlotRepository;

    @Mock
    private GroupJoinRequestRepository groupJoinRequestRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private NotificationService notificationService;

    @Mock private com.studybuddy.common.AccountAccess access;

    private StudyGroupService service;
    private Student alice;
    private Student bob;
    private Course is442;
    private StudyGroup group;

    @BeforeEach
    void setUp() {
        service = new StudyGroupService(studyGroupRepository, groupMembershipRepository,
                groupAvailabilitySlotRepository, groupJoinRequestRepository, courseRepository, new StudyGroupLookup(studyGroupRepository), new StudyGroupAssembler(),
                new GroupViewerAssembler(groupMembershipRepository, groupJoinRequestRepository),
                new GroupClosure(groupJoinRequestRepository, groupMembershipRepository, notificationService), notificationService, access);
        alice = student(ALICE_ID, "Alice");
        bob = student(BOB_ID, "Bob");
        is442 = course(IS442_ID, "IS442");
        group = group(GROUP_ID, is442, alice, 4);
        org.mockito.Mockito.lenient().when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.of(alice));
        org.mockito.Mockito.lenient().when(access.requireStudent(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(call ->
                studentRepository.findById(call.getArgument(0)).orElseThrow(() -> new StudentNotFoundException(call.getArgument(0))));
        org.mockito.Mockito.lenient().when(studentRepository.findById(BOB_ID)).thenReturn(Optional.of(bob));
    }

    // --- create ---

    @Test
    void createMakesCreatorLeaderAndFirstMemberAndSavesAvailability() {
        givenCreateDependenciesExist();

        StudyGroupDetailDto dto = service.create(ALICE_ID, details("  Midterm crammers  ", 4, List.of(MONDAY_EVENING)));

        assertEquals("Midterm crammers", dto.name());
        assertEquals(ALICE_ID, dto.leaderId());
        assertEquals(IS442_ID, dto.courseId());
        assertEquals(Set.of(StudyGoal.EXAM_PREPARATION), dto.studyGoals());
        assertEquals(StudyMode.IN_PERSON, dto.preferredStudyMode());
        assertEquals(4, dto.maxGroupSize());
        assertTrue(dto.active());
        assertEquals(1, dto.memberCount());
        assertEquals(ALICE_ID, dto.members().get(0).studentId());
        assertTrue(dto.members().get(0).leader());
        assertEquals(List.of(MONDAY_EVENING), dto.availability());
    }

    @Test
    void createSavesLeaderMembershipForTheNewGroup() {
        givenCreateDependenciesExist();

        service.create(ALICE_ID, details("Midterm crammers", 4, List.of()));

        ArgumentCaptor<GroupMembership> membership = ArgumentCaptor.forClass(GroupMembership.class);
        verify(groupMembershipRepository).save(membership.capture());
        assertEquals(alice, membership.getValue().getStudent());
        assertEquals("Midterm crammers", membership.getValue().getStudyGroup().getName());
    }

    @Test
    void createStoresBlankDescriptionAsNoDescription() {
        givenCreateDependenciesExist();
        StudyGroupDetails details = new StudyGroupDetails("Midterm crammers", "   ", IS442_ID,
                Set.of(), null, 4, List.of());

        StudyGroupDetailDto dto = service.create(ALICE_ID, details);

        assertNull(dto.description());
    }

    @Test
    void createTreatsMissingGoalsAndAvailabilityAsNone() {
        givenCreateDependenciesExist();
        StudyGroupDetails details = new StudyGroupDetails("Midterm crammers", null, IS442_ID,
                null, null, 4, null);

        StudyGroupDetailDto dto = service.create(ALICE_ID, details);

        assertTrue(dto.studyGoals().isEmpty());
        assertTrue(dto.availability().isEmpty());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(com.studybuddy.common.error.InvalidInputException.class,
                () -> service.create(ALICE_ID, details("   ", 4, List.of())));

        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createRejectsMaxSizeBelowLeaderPlusOne() {
        assertThrows(InvalidStudyGroupException.class,
                () -> service.create(ALICE_ID, details("Midterm crammers", 1, List.of())));

        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createRejectsSlotThatDoesNotStartBeforeItEnds() {
        GroupAvailabilitySlotDto backwards =
                new GroupAvailabilitySlotDto(DayOfWeek.MONDAY, LocalTime.of(20, 0), LocalTime.of(18, 0));

        assertThrows(InvalidStudyGroupException.class,
                () -> service.create(ALICE_ID, details("Midterm crammers", 4, List.of(backwards))));

        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createRejectsSlotWithMissingTime() {
        GroupAvailabilitySlotDto incomplete = new GroupAvailabilitySlotDto(DayOfWeek.MONDAY, null, LocalTime.of(18, 0));

        assertThrows(InvalidStudyGroupException.class,
                () -> service.create(ALICE_ID, details("Midterm crammers", 4, List.of(incomplete))));
    }

    @Test
    void createWithUnknownLeaderIsRejected() {
        when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.empty());

        assertThrows(StudentNotFoundException.class,
                () -> service.create(ALICE_ID, details("Midterm crammers", 4, List.of())));

        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createWithUnknownCourseIsRejected() {
        when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.of(alice));
        when(courseRepository.findById(IS442_ID)).thenReturn(Optional.empty());

        assertThrows(CourseNotFoundException.class,
                () -> service.create(ALICE_ID, details("Midterm crammers", 4, List.of())));

        verify(studyGroupRepository, never()).save(any());
    }

    // --- browse ---

    @Test
    void browseReturnsOnlyOpenGroupsMatchingTheFilterWithMemberCounts() {
        StudyGroup otherCourseGroup = group(6L, course(IS216_ID, "IS216"), bob, 3);
        when(studyGroupRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(group, otherCourseGroup));
        var count = mock(GroupMembershipRepository.GroupMemberCount.class);
        when(count.getGroupId()).thenReturn(GROUP_ID);
        when(count.getMemberCount()).thenReturn(2L);
        when(groupMembershipRepository.countByStudyGroupIds(List.of(GROUP_ID))).thenReturn(List.of(count));

        List<StudyGroupSummaryDto> results = service.browse(new StudyGroupFilter(IS442_ID, null, null), ALICE_ID);

        assertEquals(1, results.size());
        assertEquals(GROUP_ID, results.get(0).id());
        assertEquals("IS442", results.get(0).courseCode());
        assertEquals(2, results.get(0).memberCount());
        verify(groupMembershipRepository, never()).countByStudyGroupId(any());
    }

    @Test
    void browseWithNoOpenGroupsReturnsEmptyList() {
        when(studyGroupRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());

        assertTrue(service.browse(StudyGroupFilter.none(), ALICE_ID).isEmpty());
        verifyNoInteractions(groupMembershipRepository, groupJoinRequestRepository);
    }

    @Test
    void mineKeepsLedClosedGroupsWithoutMembershipsAndUsesZeroForTheirCount() {
        group.close();
        when(studyGroupRepository.findMine(ALICE_ID)).thenReturn(List.of(group));

        var result = service.mine(ALICE_ID);

        assertEquals(1, result.size());
        assertFalse(result.get(0).active());
        assertEquals(0, result.get(0).memberCount());
        assertTrue(result.get(0).viewer().leader());
        assertFalse(result.get(0).viewer().member());
    }

    // --- get ---

    @Test
    void getReturnsMembersAndAvailability() {
        givenGroupExists();
        when(groupMembershipRepository.findByStudyGroupIdOrderByJoinedAtAsc(GROUP_ID))
                .thenReturn(List.of(membership(group, alice), membership(group, bob)));
        when(groupAvailabilitySlotRepository.findByStudyGroupId(GROUP_ID))
                .thenReturn(List.of(new GroupAvailabilitySlot(group, DayOfWeek.MONDAY,
                        LocalTime.of(18, 0), LocalTime.of(20, 0))));

        StudyGroupDetailDto dto = service.get(GROUP_ID, ALICE_ID);

        assertEquals(2, dto.memberCount());
        assertTrue(dto.members().get(0).leader());
        assertFalse(dto.members().get(1).leader());
        assertEquals(List.of(MONDAY_EVENING), dto.availability());
    }

    @Test
    void onlyTheLeaderSeesHowManyApplicationsArePending() {
        givenGroupExists();
        when(groupJoinRequestRepository.countByStudyGroupIdAndStatus(GROUP_ID, GroupJoinRequestStatus.PENDING)).thenReturn(2L);
        org.mockito.Mockito.lenient().when(studentRepository.findById(BOB_ID)).thenReturn(Optional.of(bob));

        assertEquals(2L, service.get(GROUP_ID, ALICE_ID).pendingApplications());
        assertNull(service.get(GROUP_ID, BOB_ID).pendingApplications());
    }

    @Test
    void getUnknownGroupIsRejected() {
        when(studyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThrows(StudyGroupNotFoundException.class, () -> service.get(GROUP_ID, ALICE_ID));
    }

    // --- update ---

    @Test
    void leaderCanUpdateEveryFieldAndReplaceAvailability() {
        givenGroupExists();
        Course is216 = course(IS216_ID, "IS216");
        when(groupMembershipRepository.findByStudyGroupIdOrderByJoinedAtAsc(GROUP_ID))
                .thenReturn(List.of(membership(group, alice), membership(group, bob)));
        when(courseRepository.findById(IS216_ID)).thenReturn(Optional.of(is216));
        when(groupAvailabilitySlotRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        GroupAvailabilitySlotDto fridayMorning =
                new GroupAvailabilitySlotDto(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(11, 0));
        StudyGroupDetails details = new StudyGroupDetails("Finals squad", "Past papers", IS216_ID,
                Set.of(StudyGoal.PROBLEM_SOLVING), StudyMode.ONLINE, 5, List.of(fridayMorning));

        StudyGroupDetailDto dto = service.update(GROUP_ID, ALICE_ID, details);

        assertEquals("Finals squad", dto.name());
        assertEquals("Past papers", dto.description());
        assertEquals(IS216_ID, dto.courseId());
        assertEquals(Set.of(StudyGoal.PROBLEM_SOLVING), dto.studyGoals());
        assertEquals(StudyMode.ONLINE, dto.preferredStudyMode());
        assertEquals(5, dto.maxGroupSize());
        assertEquals(List.of(fridayMorning), dto.availability());
        assertEquals(2, dto.memberCount());
        verify(groupAvailabilitySlotRepository).deleteByStudyGroupId(GROUP_ID);
    }

    @Test
    void nonLeaderCannotUpdate() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class,
                () -> service.update(GROUP_ID, BOB_ID, details("Hijacked", 4, List.of())));

        assertEquals("Midterm crammers", group.getName());
        verify(groupAvailabilitySlotRepository, never()).deleteByStudyGroupId(any());
    }

    @Test
    void updateCannotShrinkMaxSizeBelowCurrentMembers() {
        givenGroupExists();
        when(groupMembershipRepository.findByStudyGroupIdOrderByJoinedAtAsc(GROUP_ID)).thenReturn(List.of(
                membership(group, alice), membership(group, bob), membership(group, student(CAROL_ID, "Carol"))));

        assertThrows(InvalidStudyGroupException.class,
                () -> service.update(GROUP_ID, ALICE_ID, details("Midterm crammers", 2, List.of())));

        assertEquals(4, group.getMaxGroupSize());
        verify(groupAvailabilitySlotRepository, never()).deleteByStudyGroupId(any());
    }

    @Test
    void closedGroupCannotBeUpdated() {
        givenGroupExists();
        group.close();

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.update(GROUP_ID, ALICE_ID, details("Reopened?", 4, List.of())));
    }

    // --- close ---

    @Test
    void leaderCanCloseGroupAndPendingRequestsAreTurnedDown() {
        givenGroupExists();
        GroupJoinRequest bobsRequest = pendingJoinRequest(20L, group, bob);
        when(groupJoinRequestRepository.findByStudyGroupIdAndStatusOrderByCreatedAtDesc(
                GROUP_ID, GroupJoinRequestStatus.PENDING)).thenReturn(List.of(bobsRequest));

        StudyGroupDetailDto dto = service.close(GROUP_ID, ALICE_ID);

        assertFalse(dto.active());
        assertFalse(group.isActive());
        assertEquals(GroupJoinRequestStatus.REJECTED, bobsRequest.getStatus());
        verify(notificationService).notify(eq(bob), eq(NotificationType.GROUP_JOIN_REQUEST_REJECTED),
                contains("closed"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonLeaderCannotClose() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class, () -> service.close(GROUP_ID, BOB_ID));

        assertTrue(group.isActive());
    }

    @Test
    void closingAClosedGroupIsRejected() {
        givenGroupExists();
        group.close();

        assertThrows(IllegalStateException.class, () -> service.close(GROUP_ID, ALICE_ID));

        verifyNoInteractions(notificationService);
    }

    // --- removeMember ---

    @Test
    void leaderCanRemoveMemberWhoIsNotified() {
        givenGroupExists();
        GroupMembership bobsMembership = membership(group, bob);
        when(groupMembershipRepository.findByStudyGroupIdAndStudentId(GROUP_ID, BOB_ID))
                .thenReturn(Optional.of(bobsMembership));

        service.removeMember(GROUP_ID, ALICE_ID, BOB_ID);

        verify(groupMembershipRepository).delete(bobsMembership);
        verify(notificationService).notify(eq(bob), eq(NotificationType.GROUP_MEMBER_REMOVED),
                contains("Midterm crammers"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void leaderCannotRemoveThemselves() {
        givenGroupExists();

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.removeMember(GROUP_ID, ALICE_ID, ALICE_ID));

        verify(groupMembershipRepository, never()).delete(any());
    }

    @Test
    void removingSomeoneWhoIsNotAMemberIsRejected() {
        givenGroupExists();
        when(groupMembershipRepository.findByStudyGroupIdAndStudentId(GROUP_ID, CAROL_ID))
                .thenReturn(Optional.empty());

        assertThrows(StudyGroupActionNotAllowedException.class,
                () -> service.removeMember(GROUP_ID, ALICE_ID, CAROL_ID));

        verifyNoInteractions(notificationService);
    }

    @Test
    void nonLeaderCannotRemoveMembers() {
        givenGroupExists();

        assertThrows(NotGroupLeaderException.class,
                () -> service.removeMember(GROUP_ID, BOB_ID, CAROL_ID));

        verify(groupMembershipRepository, never()).delete(any());
    }

    @Test
    void invalidSlotReportsAvailabilityFieldForForm() {
        var bad = new GroupAvailabilitySlotDto(DayOfWeek.MONDAY,LocalTime.of(18,0),LocalTime.of(18,0));
        var error = assertThrows(InvalidStudyGroupException.class,() -> service.create(ALICE_ID,details("Group",4,List.of(bad))));
        assertTrue(error.getFieldErrors().containsKey("availability"));
    }

    @Test
    void nullGoalsOrSlotsReturnValidationErrorsInsteadOfServerFailure() {
        var goals = new java.util.HashSet<StudyGoal>();
        goals.add(null);
        var details = new StudyGroupDetails("Group",null,IS442_ID,goals,null,4,List.of());
        assertThrows(InvalidStudyGroupException.class,() -> service.create(ALICE_ID,details));
        var nullSlot = new StudyGroupDetails("Group",null,IS442_ID,Set.of(),null,4,java.util.Arrays.asList((GroupAvailabilitySlotDto)null));
        assertThrows(InvalidStudyGroupException.class,() -> service.create(ALICE_ID,nullSlot));
    }

    @Test
    void overlongFieldsAreRejectedBeforeGroupIsSaved() {
        assertThrows(com.studybuddy.common.error.InvalidInputException.class,() -> service.create(ALICE_ID,details("x".repeat(256),4,List.of())));
        var longDescription = new StudyGroupDetails("Group","x".repeat(4001),IS442_ID,Set.of(),null,4,List.of());
        assertThrows(com.studybuddy.common.error.InvalidInputException.class,() -> service.create(ALICE_ID,longDescription));
        verify(studyGroupRepository,never()).save(any());
    }

    // --- helpers ---

    private void givenGroupExists() {
        org.mockito.Mockito.lenient().when(studyGroupRepository.findById(GROUP_ID)).thenReturn(Optional.of(group));
        org.mockito.Mockito.lenient().when(studyGroupRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(group));
    }

    private void givenCreateDependenciesExist() {
        when(studentRepository.findById(ALICE_ID)).thenReturn(Optional.of(alice));
        when(courseRepository.findById(IS442_ID)).thenReturn(Optional.of(is442));
        when(studyGroupRepository.save(any(StudyGroup.class))).thenAnswer(call -> call.getArgument(0));
        when(groupMembershipRepository.save(any(GroupMembership.class))).thenAnswer(call -> call.getArgument(0));
        when(groupAvailabilitySlotRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    private static StudyGroupDetails details(String name, int maxGroupSize, List<GroupAvailabilitySlotDto> availability) {
        return new StudyGroupDetails(name, "Weekly problem sets", IS442_ID,
                Set.of(StudyGoal.EXAM_PREPARATION), StudyMode.IN_PERSON, maxGroupSize, availability);
    }
}
