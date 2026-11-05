package com.studybuddy.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Stores students' weekly availability slots, for one student or for every candidate in a match
 * search at once.
 */
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByStudentId(Long studentId);

    List<AvailabilitySlot> findByStudentIdIn(Collection<Long> studentIds);

    void deleteByStudentId(Long studentId);
}
