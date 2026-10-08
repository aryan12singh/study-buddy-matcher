package com.studybuddy.studyroom;

import com.studybuddy.common.AccountAccess;
import com.studybuddy.common.error.ConflictException;
import com.studybuddy.common.error.ForbiddenActionException;
import com.studybuddy.studygroup.GroupMembershipRepository;
import com.studybuddy.studygroup.StudyGroup;
import com.studybuddy.studygroup.StudyGroupLookup;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Uses the same account-before-group lock order as membership management. */
@Component
public class RoomAccess {
    private final AccountAccess accounts;
    private final StudyGroupLookup groups;
    private final GroupMembershipRepository memberships;

    public RoomAccess(AccountAccess accounts, StudyGroupLookup groups, GroupMembershipRepository memberships) {
        this.accounts = accounts;
        this.groups = groups;
        this.memberships = memberships;
    }

    public StudyGroup lockMember(Long groupId, Long actorId, Long... targets) {
        Long[] ids = Stream.concat(Stream.of(actorId), Stream.of(targets)).filter(Objects::nonNull).toArray(Long[]::new);
        accounts.lockStudents(actorId, ids);
        StudyGroup group = groups.findGroupForUpdate(groupId);
        if (!group.isActive()) throw new ConflictException("This group is closed; its study room is unavailable");
        if (!memberships.existsByStudyGroupIdAndStudentId(groupId, actorId)) {
            throw new ForbiddenActionException("Only accepted group members can access this room");
        }
        return group;
    }
}
