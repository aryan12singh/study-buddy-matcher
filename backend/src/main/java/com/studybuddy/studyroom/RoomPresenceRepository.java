package com.studybuddy.studyroom;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomPresenceRepository extends JpaRepository<RoomPresence, Long> {
    Optional<RoomPresence> findByStudyRoomIdAndStudentIdAndClientId(Long roomId, Long studentId, UUID clientId);
    void deleteByStudyRoomIdAndStudentIdAndClientId(Long roomId, Long studentId, UUID clientId);
    void deleteByStudyRoomIdAndExpiresAtLessThanEqual(Long roomId, Instant now);

    @Query("""
        select p from RoomPresence p where p.studyRoom.id = :roomId and p.expiresAt > :now
        and p.student.user.active = true
        and exists (select m.id from GroupMembership m
            where m.studyGroup.id = p.studyRoom.studyGroup.id and m.student.id = p.student.id)
        order by p.observedAt desc, p.id desc
        """)
    List<RoomPresence> findEligible(@Param("roomId") Long roomId, @Param("now") Instant now);
}
