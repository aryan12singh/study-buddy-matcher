package com.studybuddy.connection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    List<Connection> findByStudentAIdOrStudentBId(Long studentAId, Long studentBId);

    /**
     * Whether the two students have an active (not ended) connection,
     * regardless of which one is stored as student A.
     */
    @Query("""
            select count(c) > 0 from Connection c
            where c.endedAt is null
              and ((c.studentA.id = :studentId and c.studentB.id = :otherStudentId)
                or (c.studentA.id = :otherStudentId and c.studentB.id = :studentId))
            """)
    boolean existsActiveBetween(@Param("studentId") Long studentId,
                                @Param("otherStudentId") Long otherStudentId);
}
