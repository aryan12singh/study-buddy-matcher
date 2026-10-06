package com.studybuddy.student;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.studygroup.GroupMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentActivityService {
    private final AccountAccess access;
    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;
    private final GroupMembershipRepository memberships;

    public StudentActivityService(AccountAccess access, ConnectionRepository connections, MatchRequestRepository requests, GroupMembershipRepository memberships) {
        this.access = access;
        this.connections = connections;
        this.requests = requests;
        this.memberships = memberships;
    }

    public StudentActivityDto summary(Long actorId) {
        access.requireStudent(actorId);
        return new StudentActivityDto(connections.countActiveByStudentId(actorId),
            requests.countByReceiverIdAndStatus(actorId, MatchRequestStatus.PENDING),
            requests.countBySenderIdAndStatus(actorId, MatchRequestStatus.PENDING), memberships.countAcceptedOpenByStudentId(actorId));
    }
}
