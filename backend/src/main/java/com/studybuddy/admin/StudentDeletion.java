package com.studybuddy.admin;

import com.studybuddy.connection.ConnectionRepository;
import com.studybuddy.matchrequest.MatchRequestRepository;
import com.studybuddy.notification.NotificationRepository;
import com.studybuddy.notification.NotificationResourceType;
import com.studybuddy.notification.NotificationService;
import com.studybuddy.notification.NotificationType;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.student.StudentRepository;
import com.studybuddy.studygroup.GroupAvailabilitySlotRepository;
import com.studybuddy.studygroup.GroupJoinRequest;
import com.studybuddy.studygroup.GroupJoinRequestRepository;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroupRepository;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

/** Explicit dependency cleanup, kept separate from reversible deactivation. */
@Component
public class StudentDeletion {
    private final StudentRepository students;
    private final AvailabilitySlotRepository studentAvailability;
    private final MatchRequestRepository requests;
    private final ConnectionRepository connections;
    private final StudyGroupRepository groups;
    private final GroupJoinRequestRepository applications;
    private final GroupMembershipRepository memberships;
    private final GroupAvailabilitySlotRepository groupAvailability;
    private final NotificationRepository notificationRows;
    private final NotificationService notifications;

    public StudentDeletion(StudentRepository students, AvailabilitySlotRepository studentAvailability, MatchRequestRepository requests,
        ConnectionRepository connections, StudyGroupRepository groups, GroupJoinRequestRepository applications,
        GroupMembershipRepository memberships, GroupAvailabilitySlotRepository groupAvailability,
        NotificationRepository notificationRows, NotificationService notifications) {
        this.students = students;
        this.studentAvailability = studentAvailability;
        this.requests = requests;
        this.connections = connections;
        this.groups = groups;
        this.applications = applications;
        this.memberships = memberships;
        this.groupAvailability = groupAvailability;
        this.notificationRows = notificationRows;
        this.notifications = notifications;
    }
    void apply(Long id) {
        var student = students.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        // Remove unsafe links before writing generic events that survive deletion.
        notificationRows.deleteAll(notificationRows.findByResourceTypeAndResourceId(NotificationResourceType.STUDENT, id));
        var buddyRequests = new LinkedHashSet<>(requests.findBySenderId(id));
        buddyRequests.addAll(requests.findByReceiverId(id));
        for (var request : buddyRequests) {
            notificationRows.deleteAll(notificationRows.findByResourceTypeAndResourceId(NotificationResourceType.MATCH_REQUEST, request.getId()));
            if (request.isPending()) {
                var other = request.getSender().getId().equals(id) ? request.getReceiver() : request.getSender();
                notifications.notify(other, NotificationType.MATCH_REQUEST_DECLINED, "A study-buddy request was closed because the other account was deleted",
                    null, null, "account-delete:" + id + ":request:" + request.getId() + ":" + other.getId());
            }
        }
        var allConnections = connections.findByStudentAIdOrStudentBId(id, id);
        for (var connection : allConnections) if (connection.isActive()) {
            var other = connection.otherStudent(id);
            notifications.notify(other, NotificationType.CONNECTION_ENDED, "Your study-buddy connection ended because the other account was deleted",
                null, null, "account-delete:" + id + ":connection:" + connection.getId() + ":" + other.getId());
        }
        requests.deleteAll(buddyRequests);
        connections.deleteAll(allConnections);
        for (var group : groups.findByLeaderId(id)) {
            notificationRows.deleteAll(notificationRows.findByResourceTypeAndResourceId(NotificationResourceType.GROUP, group.getId()));
            var members = memberships.findByStudyGroupId(group.getId());
            var groupRequests = applications.findByStudyGroupId(group.getId());
            var recipients = new LinkedHashSet<Student>();
            members.stream().map(GroupMembership::getStudent).filter(member -> !member.getId().equals(id)).forEach(recipients::add);
            groupRequests.stream().filter(GroupJoinRequest::isPending).map(GroupJoinRequest::getStudent).filter(member -> !member.getId().equals(id)).forEach(recipients::add);
            for (var recipient : recipients) notifications.notify(recipient, NotificationType.GROUP_CLOSED,
                "A study group was removed because its leader's account was deleted", null, null,
                "account-delete:" + id + ":group:" + group.getId() + ":" + recipient.getId());
            applications.deleteAll(groupRequests);
            memberships.deleteAll(members);
            groupAvailability.deleteByStudyGroupId(group.getId());
            applications.flush();
            memberships.flush();
            groupAvailability.flush();
            groups.delete(group);
            groups.flush();
        }
        for (var membership : memberships.findByStudentId(id)) {
            notifications.notify(membership.getStudyGroup().getLeader(), NotificationType.GROUP_MEMBER_REMOVED,
                "A member was removed because their account was deleted", NotificationResourceType.GROUP,
                membership.getStudyGroup().getId(), "account-delete:" + id + ":membership:" + membership.getId());
            memberships.delete(membership);
        }
        for (var application : applications.findByStudentId(id)) {
            if (application.isPending()) notifications.notify(application.getStudyGroup().getLeader(), NotificationType.GROUP_JOIN_REQUEST_REJECTED,
                "An application was withdrawn because the applicant's account was deleted", NotificationResourceType.GROUP,
                application.getStudyGroup().getId(), "account-delete:" + id + ":application:" + application.getId());
            applications.delete(application);
        }
        notificationRows.deleteAll(notificationRows.findByRecipientId(id));
        studentAvailability.deleteAll(studentAvailability.findByStudentId(id));
        student.getCoursesTaken().clear();
        student.getStudyGoals().clear();
        students.flush();
        students.delete(student);
        students.flush();
    }
}
