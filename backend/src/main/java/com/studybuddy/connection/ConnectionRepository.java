package com.studybuddy.connection;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Connection c where c.id = :id")
    Optional<Connection> findByIdForUpdate(@Param("id") Long id);
    interface Participants {
        Long getStudentAId();
        Long getStudentBId();
    }

    @Query("select c.studentA.id as studentAId, c.studentB.id as studentBId from Connection c where c.id = :id")
    Optional<Participants> findParticipants(@Param("id") Long id);

    @Query("select c from Connection c where c.endedAt is null and ((c.studentA.id=:first and c.studentB.id=:second) or (c.studentA.id=:second and c.studentB.id=:first))")
    Optional<Connection> findActiveBetween(@Param("first") Long first, @Param("second") Long second);
    List<Connection> findByStudentAIdOrStudentBId(Long studentAId, Long studentBId);

    /** The student's active connections, on either side, newest first. */
    @Query("""
            select c from Connection c
            where c.endedAt is null
              and (c.studentA.id = :studentId or c.studentB.id = :studentId)
            order by c.createdAt desc
            """)
    List<Connection> findActiveByStudentId(@Param("studentId") Long studentId);

    @Query("""
            select count(c) from Connection c
            where c.endedAt is null
              and (c.studentA.id = :studentId or c.studentB.id = :studentId)
            """)
    long countActiveByStudentId(@Param("studentId") Long studentId);
}
