package com.studybuddy.course;

import org.springframework.stereotype.Component;

/** Builds a {@link CourseDto} from a {@link Course} entity. */
@Component
public class CourseAssembler {

    public CourseDto toDto(Course course) {
        return new CourseDto(course.getId(), course.getCode(), course.getName());
    }
}
