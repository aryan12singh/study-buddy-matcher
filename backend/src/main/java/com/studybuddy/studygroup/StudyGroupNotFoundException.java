package com.studybuddy.studygroup;

import com.studybuddy.common.error.NotFoundException;

public class StudyGroupNotFoundException extends NotFoundException {

    public StudyGroupNotFoundException(Long groupId) {
        super("Study group " + groupId + " not found");
    }
}
