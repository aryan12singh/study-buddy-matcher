package com.studybuddy.studygroup;

import org.springframework.stereotype.Component;

@Component
public class GroupViewerAssembler {
    private final GroupMembershipRepository memberships;
    private final GroupJoinRequestRepository requests;

    public GroupViewerAssembler(GroupMembershipRepository memberships, GroupJoinRequestRepository requests) {
        this.memberships = memberships;
        this.requests = requests;
    }

    public GroupViewerDto assemble(StudyGroup group, Long viewerId) {
        var latest = requests.findFirstByStudyGroupIdAndStudentIdOrderByCreatedAtDescIdDesc(group.getId(), viewerId);
        return new GroupViewerDto(group.isLeader(viewerId), memberships.existsByStudyGroupIdAndStudentId(group.getId(), viewerId),
            latest.map(GroupJoinRequest::getId).orElse(null), latest.map(GroupJoinRequest::getStatus).orElse(null));
    }
}
