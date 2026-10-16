package com.studybuddy.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByStudentId(Long studentId);

    List<AvailabilitySlot> findByStudentIdIn(Collection<Long> studentIds);

    void deleteByStudentId(Long studentId);
}
