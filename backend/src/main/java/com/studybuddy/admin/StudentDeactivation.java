package com.studybuddy.admin;

import com.studybuddy.connection.ConnectionService;
import com.studybuddy.matchrequest.MatchRequestService;
import com.studybuddy.studygroup.GroupJoinRequestService;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupRepository;
import com.studybuddy.studygroup.StudyGroupService;
import org.springframework.stereotype.Component;

/**
 * What deactivating a student's account does to the rest of their data. Team
 * decision: an admin "delete" is a deactivation, never a hard delete. The
 * student's active connections end, their pending match requests (sent and
 * received) are declined, the groups they lead are closed, their own pending
 * join requests are rejected, and they are removed from every group they are
 * in. Answered requests are kept as history. Change the policy here and
 * nowhere else.
 */
@Component
public class StudentDeactivation {

    private final ConnectionService connectionService;
    private final MatchRequestService matchRequestService;
    private final GroupJoinRequestService groupJoinRequestService;
    private final StudyGroupService studyGroupService;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public StudentDeactivation(ConnectionService connectionService,
                               MatchRequestService matchRequestService,
                               GroupJoinRequestService groupJoinRequestService,
                               StudyGroupService studyGroupService,
                               StudyGroupRepository studyGroupRepository,
                               GroupMembershipRepository groupMembershipRepository) {
        this.connectionService = connectionService;
        this.matchRequestService = matchRequestService;
        this.groupJoinRequestService = groupJoinRequestService;
        this.studyGroupService = studyGroupService;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
    }

    /**
     * Groups are closed through {@link StudyGroupService#close} so pending
     * join requests are turned down and their senders told, exactly as when
     * a leader closes a group themselves.
     */
    public void apply(Long studentId) {
        connectionService.endAll(studentId);
        matchRequestService.declineAllPendingFor(studentId);
        for (StudyGroup group : studyGroupRepository.findByLeaderIdAndActiveTrue(studentId)) {
            studyGroupService.close(group.getId(), studentId);
        }
        groupJoinRequestService.rejectAllPendingFrom(studentId);
        groupMembershipRepository.deleteAll(groupMembershipRepository.findByStudentId(studentId));
    }
}
