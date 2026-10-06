package com.studybuddy.admin;

import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.matchrequest.MatchRequestStatus;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.studygroup.GroupClosure;
import com.studybuddy.studygroup.GroupJoinRequestRepository;
import com.studybuddy.studygroup.GroupJoinRequestStatus;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupNotFoundException;
import com.studybuddy.studygroup.StudyGroupRepository;
import org.springframework.stereotype.Component;

/** Package-private operation reached only after authorized exclusive lifecycle locking. */
@Component
public class StudentDeactivation {
    private final ConnectionRepository connections;
    private final MatchRequestRepository requests;
    private final GroupJoinRequestRepository applications;
    private final StudyGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final GroupClosure closure;
    private final NotificationService notifications;

    public StudentDeactivation(ConnectionRepository connections, MatchRequestRepository requests, GroupJoinRequestRepository applications,
        StudyGroupRepository groups, GroupMembershipRepository memberships, GroupClosure closure, NotificationService notifications) {
        this.connections = connections;
        this.requests = requests;
        this.applications = applications;
        this.groups = groups;
        this.memberships = memberships;
        this.closure = closure;
        this.notifications = notifications;
    }
    void apply(Long studentId) {
        for (var connection : connections.findActiveByStudentId(studentId)) {
            connection.end();
            var other = connection.otherStudent(studentId);
            notifications.notify(other, NotificationType.CONNECTION_ENDED, "Your study-buddy connection has ended because the other account is no longer active",
                NotificationResourceType.STUDENT, studentId, "connection:" + connection.getId() + ":ended:" + other.getId());
        }
        for (var request : requests.findByReceiverIdAndStatus(studentId, MatchRequestStatus.PENDING)) {
            request.decline();
            notifications.notify(request.getSender(), NotificationType.MATCH_REQUEST_DECLINED, "The other account is no longer active, so your study-buddy request was closed",
                NotificationResourceType.MATCH_REQUEST, request.getId(), "match:" + request.getId() + ":declined");
        }
        for (var request : requests.findBySenderIdAndStatus(studentId, MatchRequestStatus.PENDING)) {
            request.decline();
            notifications.notify(request.getReceiver(), NotificationType.MATCH_REQUEST_DECLINED, "The other account is no longer active, so their study-buddy request was withdrawn",
                NotificationResourceType.MATCH_REQUEST, request.getId(), "match:" + request.getId() + ":declined");
        }
        for (var group : groups.findByLeaderIdAndActiveTrue(studentId)) {
            var locked = groups.findByIdForUpdate(group.getId()).orElseThrow(() -> new StudyGroupNotFoundException(group.getId()));
            closure.close(locked);
        }
        for (var request : applications.findByStudentIdAndStatus(studentId, GroupJoinRequestStatus.PENDING)) {
            request.reject();
            notifications.notify(request.getStudyGroup().getLeader(), NotificationType.GROUP_JOIN_REQUEST_REJECTED,
                "An applicant's account is no longer active; their application was withdrawn", NotificationResourceType.GROUP,
                request.getStudyGroup().getId(), "group-request:" + request.getId() + ":withdrawn");
        }
        var current = memberships.findByStudentId(studentId);
        for (var member : current) if (!member.getStudyGroup().isLeader(studentId)) {
            notifications.notify(member.getStudyGroup().getLeader(), NotificationType.GROUP_MEMBER_REMOVED,
                "A member's account is no longer active; their membership was removed", NotificationResourceType.GROUP,
                member.getStudyGroup().getId(), "membership:" + member.getId() + ":account-removed");
        }
        memberships.deleteAll(current);
    }
}
