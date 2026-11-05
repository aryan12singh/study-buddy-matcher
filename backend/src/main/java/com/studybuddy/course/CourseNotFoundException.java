package com.studybuddy.course;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a course id does not exist. Maps to 404 Not Found. */
public class CourseNotFoundException extends NotFoundException {

    public CourseNotFoundException(Long courseId) {
        super("Course " + courseId + " not found");
    }
}
