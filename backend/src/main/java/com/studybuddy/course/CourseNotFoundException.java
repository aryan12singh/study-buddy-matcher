package com.studybuddy.course;

import com.studybuddy.common.error.NotFoundException;

public class CourseNotFoundException extends NotFoundException {

    public CourseNotFoundException(Long courseId) {
        super("Course " + courseId + " not found");
    }
}
