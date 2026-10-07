package com.studybuddy.studygroup;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    public Map<Long, GroupViewerDto> assemble(List<StudyGroup> groups, Long viewerId) {
        if (groups.isEmpty()) return Map.of();
        var groupIds = groups.stream().map(StudyGroup::getId).toList();
        var memberIds = Set.copyOf(memberships.findGroupIdsForStudent(viewerId, groupIds));
        var latest = requests.findLatestForGroups(viewerId, groupIds).stream()
            .collect(Collectors.toMap(GroupJoinRequestRepository.GroupRequestState::getGroupId, Function.identity()));
        return groups.stream().collect(Collectors.toMap(StudyGroup::getId, group -> {
            var request = latest.get(group.getId());
            return new GroupViewerDto(group.isLeader(viewerId), memberIds.contains(group.getId()),
                request == null ? null : request.getRequestId(), request == null ? null : request.getStatus());
        }));
    }
}
