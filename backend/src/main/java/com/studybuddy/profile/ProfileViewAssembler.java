package com.studybuddy.profile;

import com.studybuddy.course.Course;
import com.studybuddy.student.AvailabilitySlot;
import com.studybuddy.student.AvailabilitySlotRepository;
import com.studybuddy.student.Student;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProfileViewAssembler {
    private final ProfileRelationshipAssembler relationships;
    private final AvailabilitySlotRepository availability;

    public ProfileViewAssembler(ProfileRelationshipAssembler relationships, AvailabilitySlotRepository availability) {
        this.relationships = relationships;
        this.availability = availability;
    }

    public ProfileDto assemble(Student subject, Long viewerId) {
        var relationship = relationships.assemble(subject.getId(), viewerId);
        var slots = availability.findByStudentId(subject.getId()).stream()
            .sorted(Comparator.comparing(AvailabilitySlot::getDayOfWeek).thenComparing(AvailabilitySlot::getStartTime))
            .map(slot -> new ProfileAvailabilitySlotDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime())).toList();
        var courses = subject.getCoursesTaken().stream().sorted(Comparator.comparing(Course::getCode)).map(this::course).toList();
        if (relationship.state() == RelationshipState.SELF || relationship.state() == RelationshipState.CONNECTED) {
            return new ConnectedProfileDto(subject.getId(), subject.getName(), subject.getSchool(), subject.getProgramme(),
                subject.getYearOfStudy(), courses, course(subject.getTargetCourse()), subject.getPreferredStudyMode(),
                Set.copyOf(subject.getStudyGoals()), subject.getPreferredGroupSizeMin(), subject.getPreferredGroupSizeMax(),
                subject.getContactNumber(), slots, relationship);
        }
        return new PublicProfileDto(subject.getId(), subject.getName(), subject.getSchool(), subject.getProgramme(),
            subject.getYearOfStudy(), courses, course(subject.getTargetCourse()), subject.getPreferredStudyMode(),
            Set.copyOf(subject.getStudyGoals()), subject.getPreferredGroupSizeMin(), subject.getPreferredGroupSizeMax(), slots, relationship);
    }

    private ProfileCourseDto course(Course course) {
        return course == null ? null : new ProfileCourseDto(course.getId(), course.getCode(), course.getName());
    }
}
