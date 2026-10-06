package com.studybuddy.admin;

import com.studybuddy.auth.AccountCreation;
import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.common.error.InvalidInputException;
import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.course.Course;
import com.studybuddy.matchrequest.MatchRequest;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.studygroup.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    private static final Long ADMIN_ID = 1L;
    private static final Long BOB_ID = 2L;
    private static final Long CAROL_ID = 3L;
    @Mock private UserRepository users;
    @Mock private StudentRepository students;
    @Mock private ConnectionRepository connections;
    @Mock private MatchRequestRepository requests;
    @Mock private StudyGroupRepository groups;
    @Mock private GroupMembershipRepository memberships;
    @Mock private GroupJoinRequestRepository applications;
    @Mock private NotificationService notifications;
    @Mock private AccountCreation creation;
    @Mock private AccountAccess access;
    @Mock private StudentDeletion deletion;
    private AdminUserService service;
    private User admin;
    private User bobUser;
    private Student bob;
    private Student carol;

    @BeforeEach
    void setUp() {
        admin = user(ADMIN_ID, "admin@example.test", Role.ADMIN);
        bobUser = user(BOB_ID, "bob@example.test", Role.STUDENT);
        bob = student(bobUser, "Bob");
        carol = student(user(CAROL_ID, "carol@example.test", Role.STUDENT), "Carol");
        lenient().when(users.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        lenient().when(users.findById(BOB_ID)).thenReturn(Optional.of(bobUser));
        lenient().when(students.findById(BOB_ID)).thenReturn(Optional.of(bob));
        lenient().when(access.lockAdminTarget(anyLong(),anyLong())).thenAnswer(call -> users.findById(call.getArgument(1)).orElseThrow(() -> new UserNotFoundException(call.getArgument(1))));
        lenient().when(access.lockLifecycle(anyLong(),anyLong())).thenAnswer(call -> users.findById(call.getArgument(1)).orElseThrow(() -> new UserNotFoundException(call.getArgument(1))));
        var closure = new GroupClosure(applications, memberships, notifications);
        var deactivation = new StudentDeactivation(connections, requests, applications, groups, memberships, closure, notifications);
        var counter = new UserUsageCounter(connections, requests, groups, memberships);
        service = new AdminUserService(users,students,new AdminUserAssembler(),counter,deactivation,deletion,creation,access);
    }

    @Test
    void listFiltersAndNamesStudentsOnly() {
        when(students.findAll()).thenReturn(List.of(bob));
        when(users.findAll(any(Sort.class))).thenReturn(List.of(bobUser,admin));
        var all = service.list(AdminUserFilter.none(),ADMIN_ID);
        var filtered = service.list(new AdminUserFilter(Role.STUDENT,null,"BOB"),ADMIN_ID);
        assertEquals(2,all.size());
        assertNull(all.get(1).name());
        assertEquals(List.of(BOB_ID),filtered.stream().map(AdminUserSummaryDto::id).toList());
        verify(access,times(2)).requireAdmin(ADMIN_ID);
    }

    @Test
    void summaryCountsActualAccounts() {
        bobUser.setActive(false);
        when(users.findAll()).thenReturn(List.of(admin,bobUser));
        assertEquals(new AdminAccountsSummaryDto(2,1,1,1,1),service.summary(ADMIN_ID));
    }

    @Test
    void detailPrefillsPublicFieldsAndCurrentOpenGroupCount() {
        when(connections.countActiveByStudentId(BOB_ID)).thenReturn(2L);
        when(requests.countBySenderId(BOB_ID)).thenReturn(5L);
        when(groups.countByLeaderId(BOB_ID)).thenReturn(1L);
        when(memberships.countJoinedByStudentId(BOB_ID)).thenReturn(3L);
        when(memberships.countAcceptedOpenByStudentId(BOB_ID)).thenReturn(4L);
        var detail = service.get(BOB_ID,ADMIN_ID);
        assertEquals(new UserUsageDto(2,5,1,3,4),detail.usage());
        assertEquals("SCIS",detail.profile().school());
        assertNull(detail.account().lastLoginAt());
        assertFalse(java.util.Arrays.stream(AdminStudentProfileDto.class.getRecordComponents()).anyMatch(component -> component.getName().contains("contact")));
    }

    @Test
    void adminHasNoStudentProfileOrUsage() {
        var detail = service.get(ADMIN_ID,ADMIN_ID);
        assertNull(detail.profile());
        assertNull(detail.usage());
    }

    @Test
    void unknownUserIsNotFound() {
        assertThrows(UserNotFoundException.class,() -> service.get(99L,ADMIN_ID));
    }

    @Test
    void unauthorizedDirectServiceReadIsDenied() {
        doThrow(new ForbiddenActionException("Admin required")).when(access).requireAdmin(BOB_ID);
        assertThrows(ForbiddenActionException.class,() -> service.list(AdminUserFilter.none(),BOB_ID));
        verify(users,never()).findAll(any(Sort.class));
    }

    @Test
    void creationUsesTheSharedAccountPolicy() {
        var request = new AdminUserCreateRequest("bob@example.test","a-valid-runtime-password",Role.STUDENT,"Bob","SCIS","Information Systems",3,"Synthetic contact");
        when(creation.create(request.email(),request.password(),request.role(),request.name(),request.school(),request.programme(),request.yearOfStudy(),request.contactNumber())).thenReturn(bobUser);
        assertEquals(BOB_ID,service.create(request,ADMIN_ID).account().id());
        verify(access).lockAdmin(ADMIN_ID);
    }

    @Test
    void editNormalizesIdentityAndReplacesProfileFields() {
        var detail = service.update(BOB_ID,new AdminUserUpdateRequest(" Robert@EXAMPLE.TEST ","Robert","SOE","Economics",2,"Replacement contact"),ADMIN_ID);
        assertEquals("robert@example.test",bobUser.getEmail());
        assertEquals("SOE",detail.profile().school());
        assertEquals("Replacement contact",bob.getContactNumber());
    }

    @Test
    void omittedContactPreservesPrivateValue() {
        String previous = bob.getContactNumber();
        service.update(BOB_ID,new AdminUserUpdateRequest(bobUser.getEmail(),"Bobby","SCIS","Information Systems",3,null),ADMIN_ID);
        assertEquals(previous,bob.getContactNumber());
    }

    @Test
    void blankContactReplacementIsInvalid() {
        assertThrows(InvalidInputException.class,() -> service.update(BOB_ID,new AdminUserUpdateRequest(bobUser.getEmail(),"Bob","SCIS","IS",3," "),ADMIN_ID));
        assertEquals("Bob",bob.getName());
    }

    @Test
    void duplicateEmailDoesNotChangeAccount() {
        when(users.existsByEmail("carol@example.test")).thenReturn(true);
        assertThrows(DuplicateEmailException.class,() -> service.update(BOB_ID,new AdminUserUpdateRequest("carol@example.test","Bob","SCIS","IS",3,null),ADMIN_ID));
        assertEquals("bob@example.test",bobUser.getEmail());
    }

    @Test
    void editRejectsInvalidYearAndMissingProfile() {
        assertThrows(InvalidInputException.class,() -> service.update(BOB_ID,new AdminUserUpdateRequest(bobUser.getEmail(),"Bob","SCIS","IS",0,null),ADMIN_ID));
        assertThrows(InvalidInputException.class,() -> service.update(BOB_ID,new AdminUserUpdateRequest(bobUser.getEmail(),null,null,null,null,null),ADMIN_ID));
    }

    @Test
    void adminEditRequiresOnlyEmail() {
        service.update(ADMIN_ID,new AdminUserUpdateRequest("root@example.test",null,null,null,null,null),ADMIN_ID);
        assertEquals("root@example.test",admin.getEmail());
    }

    @Test
    void deactivationRevokesTokensAndCleansRelationshipsWithoutImpersonatingStudent() {
        var connection = new Connection(bob,carol);
        var request = new MatchRequest(carol,bob,null);
        ReflectionTestUtils.setField(request,"id",9L);
        var group = new StudyGroup("Study group",null,new Course("IS442","OOP"),bob,4);
        ReflectionTestUtils.setField(group,"id",5L);
        var member = new GroupMembership(group,bob);
        when(connections.findActiveByStudentId(BOB_ID)).thenReturn(List.of(connection));
        when(requests.findByReceiverIdAndStatus(BOB_ID,MatchRequestStatus.PENDING)).thenReturn(List.of(request));
        when(groups.findByLeaderIdAndActiveTrue(BOB_ID)).thenReturn(List.of(group));
        when(groups.findByIdForUpdate(5L)).thenReturn(Optional.of(group));
        when(memberships.findByStudentId(BOB_ID)).thenReturn(List.of(member));
        var detail = service.deactivate(BOB_ID,ADMIN_ID);
        assertFalse(detail.account().active());
        assertEquals(1,bobUser.getTokenVersion());
        assertFalse(connection.isActive());
        assertEquals(MatchRequestStatus.DECLINED,request.getStatus());
        assertFalse(group.isActive());
        verify(memberships).deleteAll(List.of(member));
        verify(access).lockLifecycle(ADMIN_ID,BOB_ID);
    }

    @Test
    void selfDeactivationAndDeletionAreBlocked() {
        assertThrows(AdminActionNotAllowedException.class,() -> service.deactivate(ADMIN_ID,ADMIN_ID));
        assertThrows(AdminActionNotAllowedException.class,() -> service.deletePermanently(ADMIN_ID,ADMIN_ID));
        assertTrue(admin.isActive());
        verify(users,never()).delete(any());
    }

    @Test
    void lastActiveAdminCannotBeRemoved() {
        var other = user(4L,"other@example.test",Role.ADMIN);
        when(users.findById(4L)).thenReturn(Optional.of(other));
        when(users.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);
        assertThrows(AdminActionNotAllowedException.class,() -> service.deactivate(4L,ADMIN_ID));
        assertTrue(other.isActive());
    }

    @Test
    void adminDeactivationTouchesNoStudentData() {
        var other = user(4L,"other@example.test",Role.ADMIN);
        when(users.findById(4L)).thenReturn(Optional.of(other));
        when(users.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(2L);
        service.deactivate(4L,ADMIN_ID);
        assertFalse(other.isActive());
        verifyNoInteractions(connections,applications,memberships,deletion);
    }

    @Test
    void repeatedDeactivationIsConflict() {
        bobUser.setActive(false);
        assertThrows(IllegalStateException.class,() -> service.deactivate(BOB_ID,ADMIN_ID));
        verifyNoInteractions(connections,applications,memberships);
    }

    @Test
    void reactivationPreservesRevocationAndDoesNotRestoreRelationships() {
        bobUser.deactivate();
        var detail = service.reactivate(BOB_ID,ADMIN_ID);
        assertTrue(detail.account().active());
        assertEquals(1,bobUser.getTokenVersion());
        verify(connections,never()).save(any());
        verify(memberships,never()).save(any());
    }

    @Test
    void repeatedReactivationIsConflict() {
        assertThrows(IllegalStateException.class,() -> service.reactivate(BOB_ID,ADMIN_ID));
    }

    @Test
    void permanentDeleteUsesSeparateCleanupAndRemovesIdentity() {
        service.deletePermanently(BOB_ID,ADMIN_ID);
        var ordered = inOrder(deletion,users);
        ordered.verify(deletion).apply(BOB_ID);
        ordered.verify(users).delete(bobUser);
        ordered.verify(users).flush();
    }

    private static User user(Long id,String email,Role role) {
        var user = new User(email,"test-hash",role);
        ReflectionTestUtils.setField(user,"id",id);
        return user;
    }
    private static Student student(User user,String name) {
        var student = new Student(user,name,"SCIS","Information Systems",3,"Synthetic contact");
        ReflectionTestUtils.setField(student,"id",user.getId());
        return student;
    }
}
