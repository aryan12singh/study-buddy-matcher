package com.studybuddy.studygroup;

public class GroupJoinRequestNotFoundException extends RuntimeException {

    public GroupJoinRequestNotFoundException(Long requestId) {
        super("Group join request " + requestId + " not found");
    }
}
