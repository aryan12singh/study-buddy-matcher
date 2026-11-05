package com.studybuddy.studysession;

import com.studybuddy.connection.Connection;
import com.studybuddy.matching.SharedSlotDto;
import com.studybuddy.matching.availability.WeeklyAvailability;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import com.studybuddy.studygroup.GroupMembership;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Suggests times for a study session by intersecting the weekly availability of everyone taking
 * part: both students of a connection, or the active members of a group.
 */
@Component
public class SessionAvailability {
    /** Weekly availability is stored as Singapore campus time, so suggestions are too. */
    private static final String AVAILABILITY_TIME_ZONE = "Asia/Singapore";

    private final AvailabilitySlotRepository slots;
    private final GroupMembershipRepository memberships;
    public SessionAvailability(AvailabilitySlotRepository slots, GroupMembershipRepository memberships) {
        this.slots = slots;
        this.memberships = memberships;
    }
    public SessionAvailabilityDto forGroup(StudyGroup group) {
        return intersect(memberships.findByStudyGroupId(group.getId()).stream().map(GroupMembership::getStudent)
            .filter(student -> student.getUser().isActive()).toList());
    }
    public SessionAvailabilityDto forConnection(Connection connection) {
        return intersect(List.of(connection.getStudentA(), connection.getStudentB()));
    }
    private SessionAvailabilityDto intersect(List<Student> students) {
        WeeklyAvailability shared = new WeeklyAvailability(List.of());
        for (int index = 0; index < students.size(); index++) {
            WeeklyAvailability next = WeeklyAvailability.from(slots.findByStudentId(students.get(index).getId()));
            shared = index == 0 ? next : new WeeklyAvailability(shared.sharedSlotsWith(next));
        }
        return new SessionAvailabilityDto(AVAILABILITY_TIME_ZONE, students.size(), shared.slots().stream()
            .map(slot -> new SharedSlotDto(slot.day(), slot.start(), slot.end())).toList(), shared.totalMinutes());
    }
}
