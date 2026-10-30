package com.studybuddy.studygroup;

import com.studybuddy.common.error.NotFoundException;

/**
 * Thrown when a group join request does not exist, does not belong to the given group, or belongs
 * to another student. Maps to 404 Not Found.
 */
public class GroupJoinRequestNotFoundException extends NotFoundException {

    public GroupJoinRequestNotFoundException(Long requestId) {
        super("Group join request " + requestId + " not found");
    }
}
