package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Stores a study group's weekly meeting slots, which are replaced as a whole when the leader edits
 * the group.
 */
public interface GroupAvailabilitySlotRepository extends JpaRepository<GroupAvailabilitySlot, Long> {

    List<GroupAvailabilitySlot> findByStudyGroupId(Long studyGroupId);

    void deleteByStudyGroupId(Long studyGroupId);
}
