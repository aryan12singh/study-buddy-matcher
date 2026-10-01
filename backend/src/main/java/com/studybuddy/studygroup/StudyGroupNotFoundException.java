package com.studybuddy.studygroup;

public class StudyGroupNotFoundException extends RuntimeException {

    public StudyGroupNotFoundException(Long groupId) {
        super("Study group " + groupId + " not found");
    }
}
