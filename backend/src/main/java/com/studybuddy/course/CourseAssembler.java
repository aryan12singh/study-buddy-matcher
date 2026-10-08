package com.studybuddy.course;

import org.springframework.stereotype.Component;

@Component
public class CourseAssembler {

    public CourseDto toDto(Course course) {
        return new CourseDto(course.getId(), course.getCode(), course.getName());
    }
}
