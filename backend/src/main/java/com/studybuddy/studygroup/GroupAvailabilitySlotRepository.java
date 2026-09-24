package com.studybuddy.studygroup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupAvailabilitySlotRepository extends JpaRepository<GroupAvailabilitySlot, Long> {

    List<GroupAvailabilitySlot> findByStudyGroupId(Long studyGroupId);
}
