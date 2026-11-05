package com.studybuddy.studysession;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.connection.Connection;
import com.studybuddy.connection.ConnectionNotFoundException;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupLookup;
import org.springframework.stereotype.Component;

/** The parent row serializes session changes with membership and connection lifecycle operations. */
@Component
public class SessionAccess {
    private final AccountAccess accounts;
    private final StudyGroupLookup groups;
    private final GroupMembershipRepository memberships;
    private final ConnectionRepository connections;
    public SessionAccess(AccountAccess accounts, StudyGroupLookup groups, GroupMembershipRepository memberships, ConnectionRepository connections) {
        this.accounts = accounts; this.groups = groups; this.memberships = memberships; this.connections = connections;
    }
    /**
     * Whether the student is a member of the session's group or one of its two buddies. A student
     * with no such tie is told the session does not exist, so session ids reveal nothing; a
     * member who may not act on it (a closed group, an ended connection) still gets 403.
     */
    public boolean isRelated(Long groupId, Long connectionId, Long actorId) {
        if (groupId != null) {
            return memberships.existsByStudyGroupIdAndStudentId(groupId, actorId);
        }
        return connections.findParticipants(connectionId)
            .map(pair -> actorId.equals(pair.getStudentAId()) || actorId.equals(pair.getStudentBId()))
            .orElse(false);
    }
    public StudyGroup readGroup(Long id, Long actorId) {
        accounts.requireStudent(actorId);
        return requireMember(groups.findGroup(id), actorId);
    }
    public StudyGroup lockGroup(Long id, Long actorId) {
        accounts.lockStudents(actorId, actorId, groups.leaderId(id));
        StudyGroup group = requireMember(groups.findGroupForUpdate(id), actorId);
        if (!group.isLeader(actorId)) throw new ForbiddenActionException("Only the group leader can manage sessions");
        return group;
    }
    private StudyGroup requireMember(StudyGroup group, Long actorId) {
        if (!group.isActive() || !memberships.existsByStudyGroupIdAndStudentId(group.getId(), actorId)) {
            throw new ForbiddenActionException("Only accepted members of an open group can access its sessions");
        }
        return group;
    }
    public Connection readConnection(Long id, Long actorId) {
        accounts.requireStudent(actorId);
        return requireParticipant(connections.findById(id).orElseThrow(() -> new ConnectionNotFoundException(id)), actorId);
    }
    public Connection lockConnection(Long id, Long actorId) {
        var pair = connections.findParticipants(id).orElseThrow(() -> new ConnectionNotFoundException(id));
        if (!actorId.equals(pair.getStudentAId()) && !actorId.equals(pair.getStudentBId())) {
            throw new ForbiddenActionException("Only connected buddies can manage their sessions");
        }
        accounts.lockStudents(actorId, pair.getStudentAId(), pair.getStudentBId());
        return requireParticipant(connections.findByIdForUpdate(id).orElseThrow(() -> new ConnectionNotFoundException(id)), actorId);
    }
    private Connection requireParticipant(Connection connection, Long actorId) {
        if (!connection.isActive() || !connection.involves(actorId)
            || !connection.getStudentA().getUser().isActive() || !connection.getStudentB().getUser().isActive()) {
            throw new ForbiddenActionException("This active buddy connection is required to access its sessions");
        }
        return connection;
    }
}
