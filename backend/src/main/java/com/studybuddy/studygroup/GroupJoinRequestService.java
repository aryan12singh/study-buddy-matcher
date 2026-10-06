package com.studybuddy.studygroup;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.InputRules;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GroupJoinRequestService {
    private final GroupJoinRequestRepository requests;
    private final GroupMembershipRepository memberships;
    private final StudyGroupLookup lookup;
    private final GroupJoinRequestAssembler assembler;
    private final NotificationService notifications;
    private final AccountAccess access;

    public GroupJoinRequestService(GroupJoinRequestRepository requests, GroupMembershipRepository memberships, StudyGroupLookup lookup,
        GroupJoinRequestAssembler assembler, NotificationService notifications, AccountAccess access) {
        this.requests = requests;
        this.memberships = memberships;
        this.lookup = lookup;
        this.assembler = assembler;
        this.notifications = notifications;
        this.access = access;
    }

    public GroupJoinRequestDto request(Long groupId, Long actorId, String message) {
        access.beginWrite();
        Long leaderId = lookup.leaderId(groupId);
        access.lockStudents(actorId, actorId, leaderId);
        StudyGroup group = lookup.findGroupForUpdate(groupId);
        var student = access.requireStudent(actorId);
        String normal = InputRules.optional(message, "Message", InputRules.TEXT_LIMIT);
        requireOpen(group);
        if (memberships.existsByStudyGroupIdAndStudentId(groupId, actorId)) {
            throw new StudyGroupActionNotAllowedException("You are already a member of this group");
        }
        if (requests.existsByStudyGroupIdAndStudentIdAndStatus(groupId, actorId, GroupJoinRequestStatus.PENDING)) {
            throw new StudyGroupActionNotAllowedException("You already have a pending request to join this group");
        }
        requireRoom(group);
        var saved = requests.save(new GroupJoinRequest(group, student, normal));
        notifications.notify(group.getLeader(), NotificationType.GROUP_JOIN_REQUEST_RECEIVED, student.getName() + " asked to join " + group.getName(),
            NotificationResourceType.GROUP, groupId, "group-request:" + saved.getId() + ":received");
        return assembler.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestDto> listPending(Long groupId, Long actorId) {
        access.requireStudent(actorId);
        lookup.findGroupLedBy(groupId, actorId);
        return requests.findByStudyGroupIdAndStatusOrderByCreatedAtDesc(groupId, GroupJoinRequestStatus.PENDING).stream().map(assembler::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestDto> mine(Long actorId) {
        access.requireStudent(actorId);
        return requests.findByStudentIdOrderByCreatedAtDescIdDesc(actorId).stream().map(assembler::toDto).toList();
    }

    public GroupJoinRequestDto accept(Long groupId, Long requestId, Long actorId) {
        var request = forDecision(groupId, requestId, actorId);
        var group = request.getStudyGroup();
        request.requirePending();
        requireOpen(group);
        if (memberships.existsByStudyGroupIdAndStudentId(groupId, request.getStudent().getId())) {
            throw new StudyGroupActionNotAllowedException("This student is already a member");
        }
        requireRoom(group);
        request.accept();
        memberships.save(new GroupMembership(group, request.getStudent()));
        notifications.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_ACCEPTED, "You have joined " + group.getName(),
            NotificationResourceType.GROUP, groupId, "group-request:" + requestId + ":accepted");
        return assembler.toDto(request);
    }

    public GroupJoinRequestDto reject(Long groupId, Long requestId, Long actorId) {
        var request = forDecision(groupId, requestId, actorId);
        requireOpen(request.getStudyGroup());
        request.reject();
        notifications.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_REJECTED, "Your request to join " + request.getStudyGroup().getName() + " was declined",
            NotificationResourceType.GROUP, groupId, "group-request:" + requestId + ":rejected");
        return assembler.toDto(request);
    }

    private GroupJoinRequest forDecision(Long groupId, Long requestId, Long actorId) {
        access.beginWrite();
        access.requireStudent(actorId);
        if (!lookup.leaderId(groupId).equals(actorId)) {
            throw new NotGroupLeaderException(groupId);
        }
        Long applicantId = requests.findApplicantId(requestId, groupId).orElseThrow(() -> new GroupJoinRequestNotFoundException(requestId));
        access.lockStudents(actorId, actorId, applicantId);
        lookup.findGroupLedByForUpdate(groupId, actorId);
        var request = requests.findByIdForUpdate(requestId).orElseThrow(() -> new GroupJoinRequestNotFoundException(requestId));
        if (!request.belongsTo(groupId)) {
            throw new GroupJoinRequestNotFoundException(requestId);
        }
        return request;
    }

    private static void requireOpen(StudyGroup group) {
        if (!group.isActive()) {
            throw new StudyGroupActionNotAllowedException("This group is closed and not taking new members");
        }
    }

    private void requireRoom(StudyGroup group) {
        if (!group.hasRoomFor(memberships.countByStudyGroupId(group.getId()))) {
            throw new StudyGroupActionNotAllowedException("This group is full");
        }
    }
}
