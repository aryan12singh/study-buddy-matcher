package com.studybuddy.admin;

import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionAssembler;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.connection.ConnectionService;
import com.studybuddy.course.Course;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupRepository;
import com.studybuddy.studygroup.StudyGroupService;
import com.studybuddy.user.Role;
import com.studybuddy.user.User;
import com.studybuddy.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Uses the real {@link UserUsageCounter}, {@link StudentDeactivation} and
 * {@link ConnectionService} over mocked repositories, so deactivation is
 * checked against the entities it changes. Closing groups and clearing
 * pending requests are delegated to mocked services, which have their own
 * tests; here we only check deactivation asks for them.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    private static final Long GROUP_ID = 5L;

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ConnectionRepository connectionRepository;
    @Mock private MatchRequestRepository matchRequestRepository;
    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private StudyGroupService studyGroupService;
    @Mock private MatchRequestService matchRequestService;
    @Mock private GroupJoinRequestService groupJoinRequestService;
    @Mock private NotificationService notificationService;

    private AdminUserService service;
    private User admin;
    private User bobUser;
    private Student bob;
    private Student carol;

    @BeforeEach
    void setUp() {
        ConnectionService connectionService =
                new ConnectionService(connectionRepository, new ConnectionAssembler(), notificationService);
        UserUsageCounter usageCounter = new UserUsageCounter(connectionRepository, matchRequestRepository,
                studyGroupRepository, groupMembershipRepository);
        StudentDeactivation deactivation = new StudentDeactivation(connectionService, matchRequestService,
                groupJoinRequestService, studyGroupService, studyGroupRepository, groupMembershipRepository);
        service = new AdminUserService(userRepository, studentRepository, new AdminUserAssembler(),
                usageCounter, deactivation);

        admin = user(ADMIN_ID, "admin@smu.edu.sg", Role.ADMIN);
        bobUser = user(BOB_ID, "bob@smu.edu.sg", Role.STUDENT);
        bob = student(bobUser, "Bob");
        carol = student(user(CAROL_ID, "carol@smu.edu.sg", Role.STUDENT), "Carol");
    }

    // --- list ---

    @Test
    void listAppliesFilterAndNamesStudentsOnly() {
        when(studentRepository.findAll()).thenReturn(List.of(bob));
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(bobUser, admin));

        List<AdminUserSummaryDto> all = service.list(AdminUserFilter.none());
        List<AdminUserSummaryDto> students = service.list(new AdminUserFilter(Role.STUDENT, null, null));

        assertEquals(2, all.size());
        assertEquals("Bob", all.get(0).name());
        assertNull(all.get(1).name());
        assertEquals(List.of(BOB_ID), students.stream().map(AdminUserSummaryDto::id).toList());
    }

    @Test
    void listSearchesStudentNamesIgnoringCase() {
        when(studentRepository.findAll()).thenReturn(List.of(bob));
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(bobUser, admin));

        List<AdminUserSummaryDto> found = service.list(new AdminUserFilter(null, null, "BOB"));

        assertEquals(List.of(BOB_ID), found.stream().map(AdminUserSummaryDto::id).toList());
    }

    // --- get and usage ---

    @Test
    void getReportsStudentUsageCounts() {
        givenUserExists(bobUser, bob);
        when(connectionRepository.countActiveByStudentId(BOB_ID)).thenReturn(2L);
        when(matchRequestRepository.countBySenderId(BOB_ID)).thenReturn(5L);
        when(studyGroupRepository.countByLeaderId(BOB_ID)).thenReturn(1L);
        when(groupMembershipRepository.countJoinedByStudentId(BOB_ID)).thenReturn(3L);

        AdminUserDetailDto detail = service.get(BOB_ID);

        assertEquals(new UserUsageDto(2, 5, 1, 3), detail.usage());
        assertEquals("bob@smu.edu.sg", detail.account().email());
        assertTrue(detail.account().active());
    }

    @Test
    void adminAccountHasNoUsage() {
        givenUserExists(admin, null);

        assertNull(service.get(ADMIN_ID).usage());
    }

    @Test
    void getUnknownUserThrowsNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.get(9L));
    }

    // --- update ---

    @Test
    void updateChangesEmailAndStudentProfile() {
        givenUserExists(bobUser, bob);
        when(userRepository.existsByEmail("robert@smu.edu.sg")).thenReturn(false);

        AdminUserDetailDto detail = service.update(BOB_ID, new AdminUserUpdateRequest(
                " robert@smu.edu.sg ", "Robert", "SOE", "Economics", 2, "+65 9111 2222"));

        assertEquals("robert@smu.edu.sg", bobUser.getEmail());
        assertEquals("Robert", detail.account().name());
        assertEquals("SOE", bob.getSchool());
        assertEquals("Economics", bob.getProgramme());
        assertEquals(2, bob.getYearOfStudy());
        assertEquals("+65 9111 2222", bob.getContactNumber());
    }

    @Test
    void updateToAnEmailAnotherAccountUsesIsRejected() {
        givenUserExists(bobUser, bob);
        when(userRepository.existsByEmail("carol@smu.edu.sg")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> service.update(BOB_ID, new AdminUserUpdateRequest(
                "carol@smu.edu.sg", "Bob", "SCIS", "Information Systems", 3, "+65 9000 0002")));

        assertEquals("bob@smu.edu.sg", bobUser.getEmail());
    }

    @Test
    void updateKeepingTheSameEmailIsNotADuplicate() {
        givenUserExists(bobUser, bob);

        service.update(BOB_ID, new AdminUserUpdateRequest(
                "bob@smu.edu.sg", "Bobby", "SCIS", "Information Systems", 3, "+65 9000 0002"));

        assertEquals("Bobby", bob.getName());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateStudentWithoutProfileFieldsIsRejected() {
        givenUserExists(bobUser, bob);

        assertThrows(InvalidAdminUserException.class, () -> service.update(BOB_ID,
                new AdminUserUpdateRequest("bob@smu.edu.sg", null, null, null, null, null)));

        assertEquals("Bob", bob.getName());
    }

    @Test
    void updateAdminNeedsOnlyAnEmail() {
        givenUserExists(admin, null);

        service.update(ADMIN_ID, new AdminUserUpdateRequest("root@smu.edu.sg", null, null, null, null, null));

        assertEquals("root@smu.edu.sg", admin.getEmail());
    }

    // --- deactivate ---

    @Test
    void deactivateEndsConnectionsClearsPendingRequestsClosesLedGroupsAndRemovesMemberships() {
        givenUserExists(bobUser, bob);
        Connection withCarol = new Connection(bob, carol);
        ReflectionTestUtils.setField(withCarol, "id", 7L);
        StudyGroup ledByBob = new StudyGroup("Midterm crammers", null, new Course("IS442", "OOP"), bob, 4);
        ReflectionTestUtils.setField(ledByBob, "id", GROUP_ID);
        List<GroupMembership> memberships = List.of(new GroupMembership(ledByBob, bob));
        when(connectionRepository.findActiveByStudentId(BOB_ID)).thenReturn(List.of(withCarol));
        when(studyGroupRepository.findByLeaderIdAndActiveTrue(BOB_ID)).thenReturn(List.of(ledByBob));
        when(groupMembershipRepository.findByStudentId(BOB_ID)).thenReturn(memberships);

        AdminUserDetailDto detail = service.deactivate(BOB_ID, ADMIN_ID);

        assertFalse(detail.account().active());
        assertFalse(bobUser.isActive());
        assertFalse(withCarol.isActive());
        verify(matchRequestService).declineAllPendingFor(BOB_ID);
        verify(groupJoinRequestService).rejectAllPendingFrom(BOB_ID);
        verify(studyGroupService).close(GROUP_ID, BOB_ID);
        verify(groupMembershipRepository).deleteAll(memberships);
    }

    @Test
    void adminCannotDeactivateThemselves() {
        assertThrows(AdminActionNotAllowedException.class, () -> service.deactivate(ADMIN_ID, ADMIN_ID));

        assertTrue(admin.isActive());
        verifyNoInteractions(userRepository, studyGroupService);
    }

    @Test
    void deactivatingAnInactiveAccountThrows() {
        bobUser.setActive(false);
        when(userRepository.findById(BOB_ID)).thenReturn(Optional.of(bobUser));

        assertThrows(IllegalStateException.class, () -> service.deactivate(BOB_ID, ADMIN_ID));

        verifyNoInteractions(studyGroupService, matchRequestService, groupJoinRequestService);
    }

    @Test
    void deactivatingAnAdminAccountTouchesNoStudentData() {
        User otherAdmin = user(4L, "ops@smu.edu.sg", Role.ADMIN);
        givenUserExists(otherAdmin, null);

        service.deactivate(4L, ADMIN_ID);

        assertFalse(otherAdmin.isActive());
        verify(connectionRepository, never()).findActiveByStudentId(anyLong());
        verifyNoInteractions(studyGroupService, matchRequestService, groupJoinRequestService);
    }

    // --- reactivate ---

    @Test
    void reactivateOnlyFlipsTheActiveFlag() {
        bobUser.setActive(false);
        givenUserExists(bobUser, bob);

        AdminUserDetailDto detail = service.reactivate(BOB_ID);

        assertTrue(detail.account().active());
        verify(connectionRepository, never()).save(any());
        verify(groupMembershipRepository, never()).save(any());
    }

    @Test
    void reactivatingAnActiveAccountThrows() {
        when(userRepository.findById(BOB_ID)).thenReturn(Optional.of(bobUser));

        assertThrows(IllegalStateException.class, () -> service.reactivate(BOB_ID));
    }

    private void givenUserExists(User user, Student student) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findById(user.getId())).thenReturn(Optional.ofNullable(student));
    }

    private static User user(Long id, String email, Role role) {
        User user = new User(email, "hashed-password", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Student student(User user, String name) {
        Student student = new Student(user, name, "SCIS", "Information Systems", 3, "+65 9000 000" + user.getId());
        ReflectionTestUtils.setField(student, "id", user.getId());
        return student;
    }
}
