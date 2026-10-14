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

@Component
public class SessionAvailability {
    private final AvailabilitySlotRepository slots;
    private final GroupMembershipRepository memberships;
    private final SessionSettings settings;
    public SessionAvailability(AvailabilitySlotRepository slots, GroupMembershipRepository memberships, SessionSettings settings) {
        this.slots = slots; this.memberships = memberships; this.settings = settings;
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
        return new SessionAvailabilityDto(settings.availabilityTimeZone(), students.size(), shared.slots().stream()
            .map(slot -> new SharedSlotDto(slot.day(), slot.start(), slot.end())).toList(), shared.totalMinutes());
    }
}
