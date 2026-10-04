package com.studybuddy.profile;

import com.studybuddy.connection.ConnectionService;
import com.studybuddy.course.Course;
import com.studybuddy.student.Student;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the profile a viewer is allowed to see. This is the privacy rule for
 * contact numbers and the only place in the codebase that copies one into a
 * response: the viewer gets {@link ConnectedProfileDto} if they are the
 * student or are actively connected to them, and {@link PublicProfileDto}
 * otherwise. Because the check runs on every view, ending a connection hides
 * the number again straight away.
 */
@Component
public class ProfileViewAssembler {

    private final ConnectionService connectionService;

    public ProfileViewAssembler(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public ProfileDto assemble(Student subject, Long viewerId) {
        if (canSeeContactNumber(subject, viewerId)) {
            return toConnected(subject);
        }
        return toPublic(subject);
    }

    private boolean canSeeContactNumber(Student subject, Long viewerId) {
        return Objects.equals(subject.getId(), viewerId)
                || connectionService.areConnected(subject.getId(), viewerId);
    }

    private PublicProfileDto toPublic(Student subject) {
        return new PublicProfileDto(
                subject.getId(),
                subject.getName(),
                subject.getSchool(),
                subject.getProgramme(),
                subject.getYearOfStudy(),
                toCourseDtos(subject.getCoursesTaken()),
                toCourseDto(subject.getTargetCourse()),
                subject.getPreferredStudyMode(),
                Set.copyOf(subject.getStudyGoals()),
                subject.getPreferredGroupSizeMin(),
                subject.getPreferredGroupSizeMax());
    }

    private ConnectedProfileDto toConnected(Student subject) {
        return new ConnectedProfileDto(
                subject.getId(),
                subject.getName(),
                subject.getSchool(),
                subject.getProgramme(),
                subject.getYearOfStudy(),
                toCourseDtos(subject.getCoursesTaken()),
                toCourseDto(subject.getTargetCourse()),
                subject.getPreferredStudyMode(),
                Set.copyOf(subject.getStudyGoals()),
                subject.getPreferredGroupSizeMin(),
                subject.getPreferredGroupSizeMax(),
                subject.getContactNumber());
    }

    /** Sorted by course code so the order is stable; the entity holds a set. */
    private List<ProfileCourseDto> toCourseDtos(Set<Course> courses) {
        return courses.stream()
                .sorted(Comparator.comparing(Course::getCode))
                .map(this::toCourseDto)
                .toList();
    }

    private ProfileCourseDto toCourseDto(Course course) {
        return course == null ? null : new ProfileCourseDto(course.getId(), course.getCode(), course.getName());
    }
}
