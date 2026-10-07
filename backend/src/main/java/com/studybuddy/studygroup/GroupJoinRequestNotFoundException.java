package com.studybuddy.studygroup;

import com.studybuddy.common.error.NotFoundException;

public class GroupJoinRequestNotFoundException extends NotFoundException {

    public GroupJoinRequestNotFoundException(Long requestId) {
        super("Group join request " + requestId + " not found");
    }
}
