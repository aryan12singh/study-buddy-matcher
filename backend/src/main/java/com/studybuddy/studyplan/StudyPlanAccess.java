package com.studybuddy.studyplan;

import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studysession.SessionAccess;
import com.studybuddy.studysession.StudySession;
import com.studybuddy.studysession.StudySessionRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Who may see and change a plan. A personal plan belongs to its student alone. A session plan
 * follows the session's own rules from {@link SessionAccess}: every current participant reads it,
 * and whoever may manage the session (the group's leader, or either buddy) also plans, ticks and
 * deletes it. Anyone else is told the plan or session does not exist, so an ID reveals nothing.
 */
@Component
public class StudyPlanAccess {
    private final SessionAccess sessionAccess;
    private final StudySessionRepository sessions;

    public StudyPlanAccess(SessionAccess sessionAccess, StudySessionRepository sessions) {
        this.sessionAccess = sessionAccess;
        this.sessions = sessions;
    }

    /** A session the actor takes part in, and whether they may manage its plan. */
    record SessionRole(StudySession session, boolean manager) { }

    public void requireReadable(StudyPlan plan, Long actorId) {
        if (plan.getScope() == StudyPlanScope.PERSONAL) {
            if (!isOwner(plan, actorId)) throw new StudyPlanNotFoundException(plan.getId());
            return;
        }
        try {
            sessionRole(plan.getSession().getId(), actorId);
        } catch (SessionPlanNotFoundException notParticipant) {
            throw new StudyPlanNotFoundException(plan.getId());
        }
    }

    /** Call after {@link #requireReadable}, which has already turned away anyone who may not see it. */
    public boolean canManage(StudyPlan plan, Long actorId) {
        return plan.getScope() == StudyPlanScope.PERSONAL
                ? isOwner(plan, actorId)
                : sessionRole(plan.getSession().getId(), actorId).manager();
    }

    /** For ticking and deleting; locks a session plan's group or connection as a session change does. */
    public void requireManageable(StudyPlan plan, Long actorId) {
        if (plan.getScope() == StudyPlanScope.PERSONAL) {
            if (!isOwner(plan, actorId)) throw new StudyPlanNotFoundException(plan.getId());
            return;
        }
        try {
            lockForManaging(plan.getSession().getId(), actorId);
        } catch (SessionPlanNotFoundException notParticipant) {
            throw new StudyPlanNotFoundException(plan.getId());
        }
    }

    /**
     * Whether the actor takes part in the session, read without locking.
     *
     * @throws SessionPlanNotFoundException when the session does not exist or they are not a current participant
     */
    SessionRole sessionRole(Long sessionId, Long actorId) {
        StudySession session = sessions.findById(sessionId).orElseThrow(() -> new SessionPlanNotFoundException(sessionId));
        try {
            if (session.getGroup() != null) {
                StudyGroup group = sessionAccess.readGroup(session.getGroup().getId(), actorId);
                return new SessionRole(session, group.isLeader(actorId));
            }
            sessionAccess.readConnection(session.getConnection().getId(), actorId);
            return new SessionRole(session, true);
        } catch (ForbiddenActionException notParticipant) {
            throw new SessionPlanNotFoundException(sessionId);
        }
    }

    /**
     * Locks the session's group or connection for a change to its plan, as the session service does
     * for a change to the session, so it cannot race a member leaving, the connection ending or the
     * session being cancelled. The session itself is read only once those locks are held.
     *
     * @throws SessionPlanNotFoundException when the actor is not a current participant
     * @throws ForbiddenActionException when a group member who is not the leader tries to plan
     */
    StudySession lockForManaging(Long sessionId, Long actorId) {
        StudySessionRepository.Parent parent = sessions.findParent(sessionId)
                .orElseThrow(() -> new SessionPlanNotFoundException(sessionId));
        try {
            if (parent.getGroupId() != null) sessionAccess.readGroup(parent.getGroupId(), actorId);
            else sessionAccess.readConnection(parent.getConnectionId(), actorId);
        } catch (ForbiddenActionException notParticipant) {
            throw new SessionPlanNotFoundException(sessionId);
        }
        if (parent.getGroupId() != null) sessionAccess.lockGroup(parent.getGroupId(), actorId);
        else sessionAccess.lockConnection(parent.getConnectionId(), actorId);
        return sessions.findByIdForUpdate(sessionId).orElseThrow(() -> new SessionPlanNotFoundException(sessionId));
    }

    private static boolean isOwner(StudyPlan plan, Long actorId) {
        return plan.getScope() == StudyPlanScope.PERSONAL && Objects.equals(plan.getStudent().getId(), actorId);
    }
}
