package com.studybuddy.admin;

import com.studybuddy.connection.ConnectionService;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupRepository;
import com.studybuddy.studygroup.StudyGroupService;
import org.springframework.stereotype.Component;

/**
 * What deactivating a student's account does to the rest of their data. Team
 * decision: an admin "delete" is a deactivation, never a hard delete. The
 * student's active connections end, the groups they lead are closed, and they
 * are removed from every group they are in. Match requests are kept as
 * history. Change the policy here and nowhere else.
 */
@Component
public class StudentDeactivation {

    private final ConnectionService connectionService;
    private final StudyGroupService studyGroupService;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;

    public StudentDeactivation(ConnectionService connectionService,
                               StudyGroupService studyGroupService,
                               StudyGroupRepository studyGroupRepository,
                               GroupMembershipRepository groupMembershipRepository) {
        this.connectionService = connectionService;
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
        for (StudyGroup group : studyGroupRepository.findByLeaderIdAndActiveTrue(studentId)) {
            studyGroupService.close(group.getId(), studentId);
        }
        groupMembershipRepository.deleteAll(groupMembershipRepository.findByStudentId(studentId));
    }
}
