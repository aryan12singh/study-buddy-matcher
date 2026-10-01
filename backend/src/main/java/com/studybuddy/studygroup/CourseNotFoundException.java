package com.studybuddy.studygroup;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long courseId) {
        super("Course " + courseId + " not found");
    }
}
