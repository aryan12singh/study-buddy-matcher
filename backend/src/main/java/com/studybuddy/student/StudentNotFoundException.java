package com.studybuddy.student;

import com.studybuddy.common.error.NotFoundException;

public class StudentNotFoundException extends NotFoundException {

    public StudentNotFoundException(Long studentId) {
        super("Student " + studentId + " not found");
    }
}
