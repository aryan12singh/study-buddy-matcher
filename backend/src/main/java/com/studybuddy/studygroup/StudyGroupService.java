package com.studybuddy.studygroup;

import com.studybuddy.course.Course;
import com.studybuddy.course.CourseRepository;
import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creating, browsing, editing and closing study groups, and removing members.
 * The acting student's id is passed in explicitly until authentication
 * supplies it. Changes to a loaded group need no explicit save: the
 * transaction flushes them on commit.
 */
@Service
@Transactional
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupAvailabilitySlotRepository groupAvailabilitySlotRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final StudyGroupLookup studyGroupLookup;
    private final StudyGroupAssembler studyGroupAssembler;
    private final NotificationService notificationService;

    public StudyGroupService(StudyGroupRepository studyGroupRepository,
                             GroupMembershipRepository groupMembershipRepository,
                             GroupAvailabilitySlotRepository groupAvailabilitySlotRepository,
                             GroupJoinRequestRepository groupJoinRequestRepository,
                             StudentRepository studentRepository,
                             CourseRepository courseRepository,
                             StudyGroupLookup studyGroupLookup,
                             StudyGroupAssembler studyGroupAssembler,
                             NotificationService notificationService) {
        this.studyGroupRepository = studyGroupRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupAvailabilitySlotRepository = groupAvailabilitySlotRepository;
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.studyGroupLookup = studyGroupLookup;
        this.studyGroupAssembler = studyGroupAssembler;
        this.notificationService = notificationService;
    }

    /**
     * Creates the group with the creator as its leader and first member, and
     * saves its weekly availability.
     *
     * @throws InvalidStudyGroupException if the details are invalid
     * @throws StudentNotFoundException if the leader does not exist
     * @throws CourseNotFoundException if the course does not exist
     */
    public StudyGroupDetailDto create(Long leaderId, StudyGroupDetails details) {
        validate(details);
        Student leader = studentRepository.findById(leaderId)
                .orElseThrow(() -> new StudentNotFoundException(leaderId));
        Course course = findCourse(details.courseId());

        StudyGroup group = new StudyGroup(details.name().strip(), normaliseText(details.description()),
                course, leader, details.maxGroupSize());
        group.setPreferredStudyMode(details.preferredStudyMode());
        group.replaceStudyGoals(details.studyGoals());
        StudyGroup saved = studyGroupRepository.save(group);

        GroupMembership leaderMembership = groupMembershipRepository.save(new GroupMembership(saved, leader));
        List<GroupAvailabilitySlot> slots = saveAvailability(saved, details.availability());
        return studyGroupAssembler.toDetail(saved, List.of(leaderMembership), slots);
    }

    /** Open groups matching the filter, newest first. */
    @Transactional(readOnly = true)
    public List<StudyGroupSummaryDto> browse(StudyGroupFilter filter) {
        return studyGroupRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(filter::matches)
                .map(group -> studyGroupAssembler.toSummary(
                        group, groupMembershipRepository.countByStudyGroupId(group.getId())))
                .toList();
    }

    /**
     * @throws StudyGroupNotFoundException if the group does not exist
     */
    @Transactional(readOnly = true)
    public StudyGroupDetailDto get(Long groupId) {
        return toDetail(studyGroupLookup.findGroup(groupId));
    }

    /**
     * Replaces every editable field, weekly availability included.
     *
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     * @throws StudyGroupActionNotAllowedException if the group is closed
     * @throws InvalidStudyGroupException if the details are invalid, or the
     *         new maximum size is below the current member count
     * @throws CourseNotFoundException if the course does not exist
     */
    public StudyGroupDetailDto update(Long groupId, Long leaderId, StudyGroupDetails details) {
        StudyGroup group = studyGroupLookup.findGroupLedBy(groupId, leaderId);
        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("A closed group cannot be edited");
        }
        validate(details);
        List<GroupMembership> memberships = groupMembershipRepository.findByStudyGroupIdOrderByJoinedAtAsc(groupId);
        if (details.maxGroupSize() < memberships.size()) {
            throw new InvalidStudyGroupException("Maximum group size cannot be below the current "
                    + memberships.size() + " members");
        }

        group.setName(details.name().strip());
        group.setDescription(normaliseText(details.description()));
        group.setCourse(findCourse(details.courseId()));
        group.setPreferredStudyMode(details.preferredStudyMode());
        group.setMaxGroupSize(details.maxGroupSize());
        group.replaceStudyGoals(details.studyGoals());

        groupAvailabilitySlotRepository.deleteByStudyGroupId(groupId);
        List<GroupAvailabilitySlot> slots = saveAvailability(group, details.availability());
        return studyGroupAssembler.toDetail(group, memberships, slots);
    }

    /**
     * Closes the group so it takes no new join requests, and turns down any
     * requests still pending so nobody is left waiting on a closed group.
     * Leaders cannot leave their group; closing it is the way out.
     *
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     * @throws IllegalStateException if the group is already closed
     */
    public StudyGroupDetailDto close(Long groupId, Long leaderId) {
        StudyGroup group = studyGroupLookup.findGroupLedBy(groupId, leaderId);
        group.close();

        List<GroupJoinRequest> pending = groupJoinRequestRepository
                .findByStudyGroupIdAndStatusOrderByCreatedAtDesc(groupId, GroupJoinRequestStatus.PENDING);
        for (GroupJoinRequest request : pending) {
            request.reject();
            notificationService.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_REJECTED,
                    group.getName() + " was closed, so your request to join it was turned down");
        }
        return toDetail(group);
    }

    /**
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     * @throws StudyGroupActionNotAllowedException if removing the leader, or
     *         the student is not a member
     */
    public void removeMember(Long groupId, Long leaderId, Long memberId) {
        StudyGroup group = studyGroupLookup.findGroupLedBy(groupId, leaderId);
        if (group.isLeader(memberId)) {
            throw new StudyGroupActionNotAllowedException(
                    "The leader cannot be removed from their own group; close the group instead");
        }
        GroupMembership membership = groupMembershipRepository.findByStudyGroupIdAndStudentId(groupId, memberId)
                .orElseThrow(() -> new StudyGroupActionNotAllowedException(
                        "Student " + memberId + " is not a member of this group"));

        groupMembershipRepository.delete(membership);
        notificationService.notify(membership.getStudent(), NotificationType.GROUP_MEMBER_REMOVED,
                "You were removed from " + group.getName());
    }

    private StudyGroupDetailDto toDetail(StudyGroup group) {
        return studyGroupAssembler.toDetail(group,
                groupMembershipRepository.findByStudyGroupIdOrderByJoinedAtAsc(group.getId()),
                groupAvailabilitySlotRepository.findByStudyGroupId(group.getId()));
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    private List<GroupAvailabilitySlot> saveAvailability(StudyGroup group, List<GroupAvailabilitySlotDto> availability) {
        List<GroupAvailabilitySlot> slots = availability.stream()
                .map(slot -> new GroupAvailabilitySlot(group, slot.dayOfWeek(), slot.startTime(), slot.endTime()))
                .toList();
        return groupAvailabilitySlotRepository.saveAll(slots);
    }

    /**
     * The same rules as the bean validation on {@link StudyGroupDetails}, plus
     * the one it cannot express: each slot must start before it ends.
     */
    private static void validate(StudyGroupDetails details) {
        if (details.name() == null || details.name().isBlank()) {
            throw new InvalidStudyGroupException("A study group needs a name");
        }
        if (details.courseId() == null) {
            throw new InvalidStudyGroupException("A study group needs a course");
        }
        if (details.maxGroupSize() == null || details.maxGroupSize() < StudyGroupDetails.MIN_GROUP_SIZE) {
            throw new InvalidStudyGroupException(
                    "Maximum group size must be at least " + StudyGroupDetails.MIN_GROUP_SIZE);
        }
        details.availability().forEach(StudyGroupService::validateSlot);
    }

    private static void validateSlot(GroupAvailabilitySlotDto slot) {
        if (slot.dayOfWeek() == null || slot.startTime() == null || slot.endTime() == null) {
            throw new InvalidStudyGroupException("Each availability slot needs a day, a start time and an end time");
        }
        if (!slot.startTime().isBefore(slot.endTime())) {
            throw new InvalidStudyGroupException("Each availability slot must start before it ends");
        }
    }

    private static String normaliseText(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
