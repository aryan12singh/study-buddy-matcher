package com.studybuddy.studygroup;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.InputRules;
import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StudyGroupService {
    private final StudyGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupAvailabilitySlotRepository availability;
    private final CourseRepository courses;
    private final StudyGroupLookup lookup;
    private final StudyGroupAssembler assembler;
    private final GroupViewerAssembler viewers;
    private final GroupClosure closure;
    private final NotificationService notifications;
    private final AccountAccess access;

    public StudyGroupService(StudyGroupRepository groups, GroupMembershipRepository memberships,
        GroupAvailabilitySlotRepository availability, CourseRepository courses, StudyGroupLookup lookup,
        StudyGroupAssembler assembler, GroupViewerAssembler viewers, GroupClosure closure,
        NotificationService notifications, AccountAccess access) {
        this.groups = groups;
        this.memberships = memberships;
        this.availability = availability;
        this.courses = courses;
        this.lookup = lookup;
        this.assembler = assembler;
        this.viewers = viewers;
        this.closure = closure;
        this.notifications = notifications;
        this.access = access;
    }

    public StudyGroupDetailDto create(Long actorId, StudyGroupDetails details) {
        access.lockStudents(actorId, actorId);
        validate(details);
        var leader = access.requireStudent(actorId);
        StudyGroup group = new StudyGroup(details.name().strip(), InputRules.optional(details.description(), "Description", InputRules.DESCRIPTION_LIMIT),
            course(details.courseId()), leader, details.maxGroupSize());
        group.setPreferredStudyMode(details.preferredStudyMode());
        group.replaceStudyGoals(details.studyGoals());
        group = groups.save(group);
        var membership = memberships.save(new GroupMembership(group, leader));
        var slots = saveAvailability(group, details.availability());
        return assembler.toDetail(group, List.of(membership), slots, new GroupViewerDto(true, true, null, null));
    }

    @Transactional(readOnly = true)
    public List<StudyGroupSummaryDto> browse(StudyGroupFilter filter, Long viewerId) {
        access.requireStudent(viewerId);
        if (filter.courseId() != null) InputRules.positiveId(filter.courseId(), "Course");
        return summaries(groups.findByActiveTrueOrderByCreatedAtDesc().stream().filter(filter::matches).toList(), viewerId);
    }

    @Transactional(readOnly = true)
    public List<StudyGroupSummaryDto> mine(Long actorId) {
        access.requireStudent(actorId);
        return summaries(groups.findMine(actorId), actorId);
    }

    @Transactional(readOnly = true)
    public StudyGroupDetailDto get(Long id, Long viewerId) {
        access.requireStudent(viewerId);
        return detail(lookup.findGroup(id), viewerId);
    }

    public StudyGroupDetailDto update(Long id, Long actorId, StudyGroupDetails details) {
        StudyGroup group = forLeaderMutation(id, actorId);
        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("A closed group cannot be edited");
        }
        validate(details);
        var current = memberships.findByStudyGroupIdOrderByJoinedAtAsc(id);
        if (details.maxGroupSize() < current.size()) {
            throw new InvalidStudyGroupException("maxGroupSize", "Maximum group size cannot be below the current " + current.size() + " members");
        }
        group.setName(details.name().strip());
        group.setDescription(InputRules.optional(details.description(), "Description", InputRules.DESCRIPTION_LIMIT));
        group.setCourse(course(details.courseId()));
        group.setPreferredStudyMode(details.preferredStudyMode());
        group.setMaxGroupSize(details.maxGroupSize());
        group.replaceStudyGoals(details.studyGoals());
        availability.deleteByStudyGroupId(id);
        availability.flush();
        var slots = saveAvailability(group, details.availability());
        return assembler.toDetail(group, current, slots, viewers.assemble(group, actorId));
    }

    public StudyGroupDetailDto close(Long id, Long actorId) {
        StudyGroup group = forLeaderMutation(id, actorId);
        closure.close(group);
        return detail(group, actorId);
    }

    public void removeMember(Long id, Long actorId, Long memberId) {
        access.beginWrite();
        InputRules.positiveId(memberId, "Member");
        access.lockStudents(actorId, actorId, memberId);
        StudyGroup group = lookup.findGroupLedByForUpdate(id, actorId);
        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("A closed group cannot be changed");
        }
        if (group.isLeader(memberId)) {
            throw new StudyGroupActionNotAllowedException("The leader cannot be removed; close the group instead");
        }
        var membership = memberships.findByStudyGroupIdAndStudentId(id, memberId).orElseThrow(() -> new StudyGroupActionNotAllowedException("This student is not a member of the group"));
        memberships.delete(membership);
        notifications.notify(membership.getStudent(), NotificationType.GROUP_MEMBER_REMOVED, "You were removed from " + group.getName(),
            NotificationResourceType.GROUP, id, "membership:" + membership.getId() + ":removed");
    }

    private StudyGroup forLeaderMutation(Long id, Long actorId) {
        access.lockStudents(actorId, actorId);
        return lookup.findGroupLedByForUpdate(id, actorId);
    }

    private List<StudyGroupSummaryDto> summaries(List<StudyGroup> listedGroups, Long viewerId) {
        if (listedGroups.isEmpty()) return List.of();
        var groupIds = listedGroups.stream().map(StudyGroup::getId).toList();
        var memberCounts = memberships.countByStudyGroupIds(groupIds).stream().collect(Collectors.toMap(
            GroupMembershipRepository.GroupMemberCount::getGroupId, GroupMembershipRepository.GroupMemberCount::getMemberCount));
        var viewerStates = viewers.assemble(listedGroups, viewerId);
        return listedGroups.stream().map(group -> assembler.toSummary(group,
            memberCounts.getOrDefault(group.getId(), 0L), viewerStates.get(group.getId()))).toList();
    }

    private StudyGroupDetailDto detail(StudyGroup group, Long viewerId) {
        return assembler.toDetail(group, memberships.findByStudyGroupIdOrderByJoinedAtAsc(group.getId()),
            availability.findByStudyGroupId(group.getId()), viewers.assemble(group, viewerId));
    }

    private Course course(Long id) {
        return courses.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }

    private List<GroupAvailabilitySlot> saveAvailability(StudyGroup group, List<GroupAvailabilitySlotDto> slots) {
        return availability.saveAll(slots.stream().map(slot -> new GroupAvailabilitySlot(group, slot.dayOfWeek(), slot.startTime(), slot.endTime())).toList());
    }

    private static void validate(StudyGroupDetails details) {
        if (details == null) {
            throw new InvalidStudyGroupException("Group details are required");
        }
        InputRules.required(details.name(), "Name", InputRules.TEXT_LIMIT);
        InputRules.optional(details.description(), "Description", InputRules.DESCRIPTION_LIMIT);
        InputRules.positiveId(details.courseId(), "Course");
        if (details.maxGroupSize() == null || details.maxGroupSize()<StudyGroupDetails.MIN_GROUP_SIZE) {
            throw new InvalidStudyGroupException("maxGroupSize", "Maximum group size must be at least 2");
        }
        if (details.studyGoals().stream().anyMatch(Objects::isNull)) {
            throw new InvalidStudyGroupException("studyGoals", "Study goals cannot contain a null value");
        }
        for (var slot : details.availability()) {
            if (slot == null || slot.dayOfWeek() == null || slot.startTime() == null || slot.endTime() == null) {
                throw new InvalidStudyGroupException("availability", "Each availability slot needs a day, start time and end time");
            }
            if (!slot.startTime().isBefore(slot.endTime())) {
                throw new InvalidStudyGroupException("availability", "Each availability slot must start before it ends");
            }
            if (slot.startTime().getSecond() != 0 || slot.startTime().getNano() != 0 || slot.endTime().getSecond() != 0 || slot.endTime().getNano() != 0) {
                throw new InvalidStudyGroupException("availability", "Weekly times must use whole minutes");
            }
        }
    }
}
