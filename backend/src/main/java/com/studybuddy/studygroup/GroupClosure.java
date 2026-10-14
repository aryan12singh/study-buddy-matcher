package com.studybuddy.studygroup;

import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.studyroom.RoomLifecycle;
import com.studybuddy.studysession.StudySessionLifecycle;
import org.springframework.stereotype.Component;

/** Internal transition reused by the leader action and authorized account cleanup. */
@Component
public class GroupClosure {
    private final GroupJoinRequestRepository requests;
    private final GroupMembershipRepository memberships;
    private final NotificationService notifications;
    private final RoomLifecycle roomLifecycle;

    private final StudySessionLifecycle sessionLifecycle;

    public GroupClosure(GroupJoinRequestRepository requests, GroupMembershipRepository memberships,
        NotificationService notifications, RoomLifecycle roomLifecycle, StudySessionLifecycle sessionLifecycle) {
        this.requests = requests;
        this.memberships = memberships;
        this.notifications = notifications;
        this.sessionLifecycle = sessionLifecycle;
        this.roomLifecycle = roomLifecycle;
    }

    public void close(StudyGroup group) {
        sessionLifecycle.cancelForGroup(group.getId());
        group.close();
        roomLifecycle.close(group.getId());
        for (var request : requests.findByStudyGroupIdAndStatusOrderByCreatedAtDesc(group.getId(), GroupJoinRequestStatus.PENDING)) {
            request.reject();
            notifications.notify(request.getStudent(), NotificationType.GROUP_JOIN_REQUEST_REJECTED,
                group.getName() + " was closed, so your request to join it was turned down", NotificationResourceType.GROUP,
                group.getId(), "group-request:" + request.getId() + ":rejected");
        }
        for (var member : memberships.findByStudyGroupId(group.getId())) {
            if (!group.isLeader(member.getStudent().getId())) notifications.notify(member.getStudent(), NotificationType.GROUP_CLOSED,
                group.getName() + " was closed", NotificationResourceType.GROUP, group.getId(),
                "group:" + group.getId() + ":closed:" + member.getStudent().getId());
        }
    }
}
