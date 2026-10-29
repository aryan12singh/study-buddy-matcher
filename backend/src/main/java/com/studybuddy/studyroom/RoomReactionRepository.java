package com.studybuddy.studyroom;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomReactionRepository extends JpaRepository<RoomReaction, Long> {
    List<RoomReaction> findByStudyRoomIdAndSentAtAfterOrderByIdAsc(Long roomId, Instant after);
    long countByStudyRoomIdAndStudentIdAndSentAtAfter(Long roomId, Long studentId, Instant after);
    void deleteByStudyRoomIdAndSentAtBefore(Long roomId, Instant before);
}
