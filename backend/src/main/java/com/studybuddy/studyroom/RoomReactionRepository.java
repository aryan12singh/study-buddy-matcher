package com.studybuddy.studyroom;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomReactionRepository extends JpaRepository<RoomReaction, Long> {
    List<RoomReaction> findByStudyRoomIdAndSentAtAfterOrderByIdAsc(Long roomId, Instant after);
    long countByStudyRoomIdAndStudentIdAndSentAtAfter(Long roomId, Long studentId, Instant after);
    /** One bulk statement, so two members reacting at once cannot both try to remove the same rows. */
    @Modifying
    @Query("delete from RoomReaction r where r.studyRoom.id = :roomId and r.sentAt < :before")
    void deleteOlderThan(@Param("roomId") Long roomId, @Param("before") Instant before);
}
