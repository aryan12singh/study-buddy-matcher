package com.studybuddy.studygroup;

import org.springframework.stereotype.Component;

/**
 * Loads a study group and, for leader-only actions, checks the acting student
 * leads it. Shared by the group and join request services so the leader rule
 * is written once.
 */
@Component
public class StudyGroupLookup {

    private final StudyGroupRepository studyGroupRepository;

    public StudyGroupLookup(StudyGroupRepository studyGroupRepository) {
        this.studyGroupRepository = studyGroupRepository;
    }

    /**
     * @throws StudyGroupNotFoundException if the group does not exist
     */
    public StudyGroup findGroup(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new StudyGroupNotFoundException(groupId));
    }

    /**
     * @throws StudyGroupNotFoundException if the group does not exist
     * @throws NotGroupLeaderException if the student does not lead the group
     */
    public StudyGroup findGroupLedBy(Long groupId, Long studentId) {
        StudyGroup group = findGroup(groupId);
        if (!group.isLeader(studentId)) {
            throw new NotGroupLeaderException(groupId);
        }
        return group;
    }
}
