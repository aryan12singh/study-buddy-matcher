package com.studybuddy.quiz;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupLookup;
import com.studybuddy.studyroom.StudyRoom;
import com.studybuddy.studyroom.StudyRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuizAccessTest {
    private static final long GROUP = 5L;
    private static final long QUIZ = 50L;
    private static final long LEADER = 1L;
    private static final long HOST = 2L;
    private static final long CO_HOST = 3L;
    private static final long MEMBER = 4L;
    private static final long OUTSIDER = 9L;

    private final StudyGroupLookup groups = mock(StudyGroupLookup.class);
    private final GroupMembershipRepository memberships = mock(GroupMembershipRepository.class);
    private final StudyRoomRepository rooms = mock(StudyRoomRepository.class);
    private final QuizAccess access = new QuizAccess(mock(AccountAccess.class), groups, memberships, rooms);
    private final StudyGroup group = mock(StudyGroup.class);
    private final Quiz quiz = mock(Quiz.class);

    private static Student student(long id) {
        Student student = mock(Student.class);
        when(student.getId()).thenReturn(id);
        return student;
    }

    @BeforeEach
    void anOpenGroupWithARoom() {
        when(group.getId()).thenReturn(GROUP);
        when(group.isActive()).thenReturn(true);
        when(group.isLeader(LEADER)).thenReturn(true);
        when(groups.findGroup(GROUP)).thenReturn(group);
        for (long member : new long[]{LEADER, HOST, CO_HOST, MEMBER}) {
            when(memberships.existsByStudyGroupIdAndStudentId(GROUP, member)).thenReturn(true);
        }
        StudyRoom room = mock(StudyRoom.class);
        Student host = student(HOST);
        Student coHost = student(CO_HOST);
        when(room.getHost()).thenReturn(host);
        when(room.getCoHost()).thenReturn(coHost);
        when(rooms.findByStudyGroupId(GROUP)).thenReturn(Optional.of(room));
        when(quiz.getId()).thenReturn(QUIZ);
        when(quiz.getScope()).thenReturn(QuizScope.GROUP);
        when(quiz.getStudyGroup()).thenReturn(group);
    }

    @Test
    void theLeaderHostAndCoHostCanHostButAPlainMemberCannot() {
        assertTrue(access.canHost(group, LEADER));
        assertTrue(access.canHost(group, HOST));
        assertTrue(access.canHost(group, CO_HOST));
        assertFalse(access.canHost(group, MEMBER));
    }

    @Test
    void onlyTheLeaderCanHostWhenTheGroupHasNoRoomYet() {
        when(rooms.findByStudyGroupId(GROUP)).thenReturn(Optional.empty());

        assertTrue(access.canHost(group, LEADER));
        assertFalse(access.canHost(group, HOST));
    }

    @Test
    void aRoomWithoutACoHostGivesNobodyThatRole() {
        StudyRoom room = mock(StudyRoom.class);
        Student host = student(HOST);
        when(room.getHost()).thenReturn(host);
        when(rooms.findByStudyGroupId(GROUP)).thenReturn(Optional.of(room));

        assertFalse(access.canHost(group, CO_HOST));
        assertTrue(access.canHost(group, HOST));
    }

    @Test
    void membersSeeTheGroupsQuizzesAndOutsidersAreRefused() {
        assertSame(group, access.requireMember(GROUP, MEMBER));
        assertThrows(ForbiddenActionException.class, () -> access.requireMember(GROUP, OUTSIDER));
    }

    @Test
    void nobodySeesTheQuizzesOfAClosedGroup() {
        when(group.isActive()).thenReturn(false);

        assertThrows(ForbiddenActionException.class, () -> access.requireMember(GROUP, LEADER));
        assertThrows(QuizNotFoundException.class, () -> access.requireReadable(quiz, LEADER));
    }

    @Test
    void onlyHostsCanStartAQuiz() {
        assertSame(group, access.requireHost(GROUP, HOST));
        assertThrows(ForbiddenActionException.class, () -> access.requireHost(GROUP, MEMBER));
        assertThrows(ForbiddenActionException.class, () -> access.requireHost(GROUP, OUTSIDER));
    }

    @Test
    void anOutsiderIsToldAQuizDoesNotExist() {
        assertDoesNotThrow(() -> access.requireReadable(quiz, MEMBER));
        assertThrows(QuizNotFoundException.class, () -> access.requireReadable(quiz, OUTSIDER));
        assertThrows(QuizNotFoundException.class, () -> access.requireClosable(quiz, OUTSIDER));
    }

    @Test
    void aMemberWhoCannotHostIsToldWhyTheyCannotCloseAQuiz() {
        assertDoesNotThrow(() -> access.requireClosable(quiz, CO_HOST));
        assertThrows(ForbiddenActionException.class, () -> access.requireClosable(quiz, MEMBER));
    }

    @Test
    void onlyItsOwnerSeesOrClosesAPersonalQuiz() {
        Quiz personal = mock(Quiz.class);
        when(personal.getId()).thenReturn(60L);
        when(personal.getScope()).thenReturn(QuizScope.PERSONAL);
        when(personal.isOwnedBy(MEMBER)).thenReturn(true);

        assertDoesNotThrow(() -> access.requireReadable(personal, MEMBER));
        assertDoesNotThrow(() -> access.requireClosable(personal, MEMBER));
        assertTrue(access.canHost(personal, MEMBER));
        assertThrows(QuizNotFoundException.class, () -> access.requireReadable(personal, LEADER));
        assertThrows(QuizNotFoundException.class, () -> access.requireClosable(personal, LEADER));
        assertFalse(access.canHost(personal, LEADER));
    }
}
