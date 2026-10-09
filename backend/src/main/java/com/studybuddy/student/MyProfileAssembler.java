package com.studybuddy.student;

import com.studybuddy.course.Course;
import com.studybuddy.course.CourseAssembler;
import com.studybuddy.course.CourseDto;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MyProfileAssembler {
    private final CourseAssembler courses;
    private final ProfileProperties properties;

    public MyProfileAssembler(CourseAssembler courses, ProfileProperties properties) {
        this.courses = courses;
        this.properties = properties;
    }

    public MyProfileDto toDto(Student student, List<AvailabilitySlot> slots) {
        List<CourseDto> taken = student.getCoursesTaken().stream()
                .sorted(Comparator.comparing(Course::getCode))
                .map(courses::toDto)
                .toList();
        CourseDto target = student.getTargetCourse() == null ? null : courses.toDto(student.getTargetCourse());
        return new MyProfileDto(
                student.getId(),
                student.getUser().getEmail(),
                student.getName(),
                student.getSchool(),
                student.getProgramme(),
                student.getYearOfStudy(),
                student.getContactNumber(),
                taken,
                target,
                student.getPreferredStudyMode(),
                GroupSizePreference.fromRange(student.getPreferredGroupSizeMin(), student.getPreferredGroupSizeMax()),
                properties.groupSizeMax(),
                Set.copyOf(student.getStudyGoals()),
                toSlotDtos(slots));
    }

    /** Monday first, then by start time, so the week reads in order. */
    public List<AvailabilitySlotDto> toSlotDtos(List<AvailabilitySlot> slots) {
        return slots.stream()
                .sorted(Comparator.comparing(AvailabilitySlot::getDayOfWeek)
                        .thenComparing(AvailabilitySlot::getStartTime))
                .map(slot -> new AvailabilitySlotDto(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime()))
                .toList();
    }
}
