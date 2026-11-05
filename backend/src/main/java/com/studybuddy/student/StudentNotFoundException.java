package com.studybuddy.student;

import com.studybuddy.common.error.NotFoundException;

/** Thrown when a student id does not exist or is not an active student. Maps to 404 Not Found. */
public class StudentNotFoundException extends NotFoundException {

    public StudentNotFoundException(Long studentId) {
        super("Student " + studentId + " not found");
    }
}
