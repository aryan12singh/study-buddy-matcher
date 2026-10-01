package com.studybuddy.studygroup;

import com.studybuddy.matchrequest.StudentNotFoundException;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asking to join a study group and the leader's answer. The acting student's
 * id is passed in explicitly until authentication supplies it. Status changes
 * on a loaded request need no explicit save: the transaction flushes them on
 * commit.
 */
@Service
@Transactional
public class GroupJoinRequestService {

    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final StudentRepository studentRepository;
    private final StudyGroupLookup studyGroupLookup;
    private final GroupJoinRequestAssembler groupJoinRequestAssembler;
    private final NotificationService notificationService;

    public GroupJoinRequestService(GroupJoinRequestRepository groupJoinRequestRepository,
                                   GroupMembershipRepository groupMembershipRepository,
                                   StudentRepository studentRepository,
                                   StudyGroupLookup studyGroupLookup,
                                   GroupJoinRequestAssembler groupJoinRequestAssembler,
                                   NotificationService notificationService) {
        this.groupJoinRequestRepository = groupJoinRequestRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.studentRepository = studentRepository;
        this.studyGroupLookup = studyGroupLookup;
        this.groupJoinRequestAssembler = groupJoinRequestAssembler;
        this.notificationService = notificationService;
    }

    /**
     * @param message optional; blank is stored as no message
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws StudentNotFoundException if the student does not exist
     * @throws StudyGroupActionNotAllowedException if the group is closed or
     *         full, the student is already a member, or already has a request
     *         pending for this group
     */
    public GroupJoinRequestDto request(Long groupId, Long studentId, String message) {
        StudyGroup group = studyGroupLookup.findGroup(groupId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("This group is closed and not taking new members");
        }
        if (groupMembershipRepository.existsByStudyGroupIdAndStudentId(groupId, studentId)) {
            throw new StudyGroupActionNotAllowedException("You are already a member of this group");
        }
        if (groupJoinRequestRepository.existsByStudyGroupIdAndStudentIdAndStatus(
                groupId, studentId, GroupJoinRequestStatus.PENDING)) {
            throw new StudyGroupActionNotAllowedException("You already have a pending request to join this group");
        }
        requireRoom(group);

        GroupJoinRequest saved = groupJoinRequestRepository.save(
                new GroupJoinRequest(group, student, normaliseMessage(message)));
        notificationService.notify(group.getLeader(), NotificationType.GROUP_JOIN_REQUEST_RECEIVED,
                student.getName() + " asked to join " + group.getName());
        return groupJoinRequestAssembler.toDto(saved);
    }

    /**
     * Pending requests for the group, newest first.
     *
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     */
    @Transactional(readOnly = true)
    public List<GroupJoinRequestDto> listPending(Long groupId, Long leaderId) {
        studyGroupLookup.findGroupLedBy(groupId, leaderId);
        return groupJoinRequestRepository
                .findByStudyGroupIdAndStatusOrderByCreatedAtDesc(groupId, GroupJoinRequestStatus.PENDING)
                .stream()
                .map(groupJoinRequestAssembler::toDto)
                .toList();
    }

    /**
     * Accepts the request and adds the student to the group, if it has room.
     *
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     * @throws GroupJoinRequestNotFoundException if the request does not exist
     *         in this group
     * @throws IllegalStateException if the request is no longer pending,
     *         checked first so a repeated accept is not reported as "full"
     * @throws StudyGroupActionNotAllowedException if the group is closed or full
     */
    public GroupJoinRequestDto accept(Long groupId, Long requestId, Long leaderId) {
        StudyGroup group = studyGroupLookup.findGroupLedBy(groupId, leaderId);
        GroupJoinRequest request = findRequestInGroup(requestId, groupId);
        request.requirePending();
        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("This group is closed and not taking new members");
        }
        requireRoom(group);

        request.accept();
        groupMembershipRepository.save(new GroupMembership(group, request.getStudent()));
        notificationService.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_ACCEPTED,
                "You have joined " + group.getName());
        return groupJoinRequestAssembler.toDto(request);
    }

    /**
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     * @throws GroupJoinRequestNotFoundException if the request does not exist
     *         in this group
     * @throws IllegalStateException if the request is no longer pending
     */
    public GroupJoinRequestDto reject(Long groupId, Long requestId, Long leaderId) {
        StudyGroup group = studyGroupLookup.findGroupLedBy(groupId, leaderId);
        GroupJoinRequest request = findRequestInGroup(requestId, groupId);

        request.reject();
        notificationService.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_REJECTED,
                "Your request to join " + group.getName() + " was declined");
        return groupJoinRequestAssembler.toDto(request);
    }

    private void requireRoom(StudyGroup group) {
        if (!group.hasRoomFor(groupMembershipRepository.countByStudyGroupId(group.getId()))) {
            throw new StudyGroupActionNotAllowedException("This group is full");
        }
    }

    /** A request reached through another group's URL is treated as not found. */
    private GroupJoinRequest findRequestInGroup(Long requestId, Long groupId) {
        GroupJoinRequest request = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GroupJoinRequestNotFoundException(requestId));
        if (!request.belongsTo(groupId)) {
            throw new GroupJoinRequestNotFoundException(requestId);
        }
        return request;
    }

    private static String normaliseMessage(String message) {
        return message == null || message.isBlank() ? null : message.strip();
    }
}
