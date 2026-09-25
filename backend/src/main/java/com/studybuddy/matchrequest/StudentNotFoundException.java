package com.studybuddy.matchrequest;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long studentId) {
        super("Student " + studentId + " not found");
    }
}
