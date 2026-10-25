package com.studybuddy.quiz;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupLookup;
import com.studybuddy.studyroom.StudyRoomRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Who may do what with a quiz. A personal quiz belongs to its student alone. A group quiz reuses the
 * group and study room rules: accepted members of an open group may see and take it, and the group
 * leader and the room's host and co-host may also start and close them. A quiz the actor may not
 * see is reported as not found, so its ID reveals nothing; group pages are already visible, so a
 * group's quiz list is refused with 403 like its room and sessions.
 */
@Component
public class QuizAccess {
    private final AccountAccess accounts;
    private final StudyGroupLookup groups;
    private final GroupMembershipRepository memberships;
    private final StudyRoomRepository rooms;

    public QuizAccess(AccountAccess accounts, StudyGroupLookup groups, GroupMembershipRepository memberships,
            StudyRoomRepository rooms) {
        this.accounts = accounts;
        this.groups = groups;
        this.memberships = memberships;
        this.rooms = rooms;
    }

    /** For listing a group's quizzes. */
    public StudyGroup requireMember(Long groupId, Long actorId) {
        accounts.requireStudent(actorId);
        StudyGroup group = groups.findGroup(groupId);
        if (!isMember(group, actorId)) {
            throw new ForbiddenActionException("Only accepted members of an open group can see its quizzes");
        }
        return group;
    }

    /** For starting a quiz in a group. */
    public StudyGroup requireHost(Long groupId, Long actorId) {
        StudyGroup group = requireMember(groupId, actorId);
        requireHostOf(group, actorId);
        return group;
    }

    /** For seeing, taking and reviewing one quiz. */
    public void requireReadable(Quiz quiz, Long actorId) {
        accounts.requireStudent(actorId);
        boolean readable = quiz.getScope() == QuizScope.PERSONAL
                ? quiz.isOwnedBy(actorId)
                : isMember(quiz.getStudyGroup(), actorId);
        if (!readable) {
            throw new QuizNotFoundException(quiz.getId());
        }
    }

    /** For closing one quiz: a member who may not host is told why, not that the quiz is missing. */
    public void requireClosable(Quiz quiz, Long actorId) {
        requireReadable(quiz, actorId);
        if (quiz.getScope() == QuizScope.GROUP) {
            requireHostOf(quiz.getStudyGroup(), actorId);
        }
    }

    /** Whether the actor may close this quiz: its owner, or someone who may host its group's quizzes. */
    public boolean canHost(Quiz quiz, Long actorId) {
        return quiz.getScope() == QuizScope.PERSONAL ? quiz.isOwnedBy(actorId) : canHost(quiz.getStudyGroup(), actorId);
    }

    /** Whether the actor may start and close quizzes in this group; shown to the screen as a flag. */
    public boolean canHost(StudyGroup group, Long actorId) {
        if (group.isLeader(actorId)) {
            return true;
        }
        return rooms.findByStudyGroupId(group.getId())
                .map(room -> isPerson(room.getHost(), actorId) || isPerson(room.getCoHost(), actorId))
                .orElse(false);
    }

    private void requireHostOf(StudyGroup group, Long actorId) {
        if (!canHost(group, actorId)) {
            throw new ForbiddenActionException("Only the group leader or the study room's host or co-host can start or close a quiz");
        }
    }

    private boolean isMember(StudyGroup group, Long actorId) {
        return group.isActive() && memberships.existsByStudyGroupIdAndStudentId(group.getId(), actorId);
    }

    private static boolean isPerson(Student student, Long actorId) {
        return student != null && Objects.equals(student.getId(), actorId);
    }
}
