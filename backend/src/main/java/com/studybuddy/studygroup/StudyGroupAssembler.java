package com.studybuddy.studygroup;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Converts {@link StudyGroup} entities, with their members and availability,
 * into DTOs so the entities never leave the service layer.
 */
@Component
public class StudyGroupAssembler {

    /**
     * Monday first, then by start time. Sorted here rather than in the query:
     * day_of_week is stored as text, so the database would sort it
     * alphabetically and put FRIDAY before MONDAY.
     */
    private static final Comparator<GroupAvailabilitySlot> WEEK_ORDER =
            Comparator.comparing(GroupAvailabilitySlot::getDayOfWeek)
                    .thenComparing(GroupAvailabilitySlot::getStartTime);

    public StudyGroupSummaryDto toSummary(StudyGroup group, long memberCount) {
        return new StudyGroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getCourse().getId(),
                group.getCourse().getCode(),
                group.getCourse().getName(),
                group.getLeader().getId(),
                group.getLeader().getName(),
                group.getPreferredStudyMode(),
                Set.copyOf(group.getStudyGoals()),
                group.getMaxGroupSize(),
                memberCount,
                group.isActive());
    }

    public StudyGroupDetailDto toDetail(StudyGroup group,
                                        List<GroupMembership> memberships,
                                        List<GroupAvailabilitySlot> slots) {
        return new StudyGroupDetailDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCourse().getId(),
                group.getCourse().getCode(),
                group.getCourse().getName(),
                group.getLeader().getId(),
                group.getLeader().getName(),
                group.getPreferredStudyMode(),
                Set.copyOf(group.getStudyGoals()),
                group.getMaxGroupSize(),
                memberships.size(),
                group.isActive(),
                group.getCreatedAt(),
                slots.stream().sorted(WEEK_ORDER).map(this::toSlotDto).toList(),
                memberships.stream().map(membership -> toMemberDto(group, membership)).toList());
    }

    private GroupAvailabilitySlotDto toSlotDto(GroupAvailabilitySlot slot) {
        return new GroupAvailabilitySlotDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
    }

    private GroupMemberDto toMemberDto(StudyGroup group, GroupMembership membership) {
        Long studentId = membership.getStudent().getId();
        return new GroupMemberDto(
                studentId,
                membership.getStudent().getName(),
                group.isLeader(studentId),
                membership.getJoinedAt());
    }
}
