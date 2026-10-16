package com.studybuddy.studyroom;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    Optional<StudyRoom> findByStudyGroupId(Long groupId);
    List<StudyRoom> findByHostIdOrCoHostId(Long hostId, Long coHostId);
}
