package com.studybuddy.admin;

import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupRepository;
import org.springframework.stereotype.Component;

/**
 * Decides what "basic usage information" means on the admin screen. Team
 * decision: last successful sign-in (on the account DTO), active connections,
 * and accepted memberships in open groups, including groups the student leads.
 * Requests sent, groups led and groups joined are extra history counts that the
 * screen labels separately. Change the definition here and nowhere else.
 */
@Component
public class UserUsageCounter {
    private final ConnectionRepository connectionRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public UserUsageCounter(ConnectionRepository connectionRepository,
        MatchRequestRepository matchRequestRepository,
        StudyGroupRepository studyGroupRepository,
        GroupMembershipRepository groupMembershipRepository) {
        this.connectionRepository = connectionRepository;
        this.matchRequestRepository = matchRequestRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    public UserUsageDto countFor(Long studentId) {
        return new UserUsageDto(
            connectionRepository.countActiveByStudentId(studentId),
            matchRequestRepository.countBySenderId(studentId),
            studyGroupRepository.countByLeaderId(studentId),
            groupMembershipRepository.countJoinedByStudentId(studentId),
            groupMembershipRepository.countAcceptedOpenByStudentId(studentId));
    }
}
