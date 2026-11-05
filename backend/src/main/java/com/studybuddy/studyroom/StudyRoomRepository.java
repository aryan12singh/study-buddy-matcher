package com.studybuddy.studyroom;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Finds a group's room, and the rooms where a student is host or co-host so those roles can be
 * cleared when the student is deactivated.
 */
public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    Optional<StudyRoom> findByStudyGroupId(Long groupId);
    List<StudyRoom> findByHostIdOrCoHostId(Long hostId, Long coHostId);
}
