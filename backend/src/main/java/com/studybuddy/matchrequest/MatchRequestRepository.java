package com.studybuddy.matchrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    List<MatchRequest> findBySenderId(Long senderId);

    List<MatchRequest> findByReceiverId(Long receiverId);

    List<MatchRequest> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    List<MatchRequest> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    /**
     * Whether a PENDING request exists between the two students, sent in
     * either direction.
     */
    @Query("""
            select count(r) > 0 from MatchRequest r
            where r.status = com.studybuddy.matchrequest.MatchRequestStatus.PENDING
              and ((r.sender.id = :studentId and r.receiver.id = :otherStudentId)
                or (r.sender.id = :otherStudentId and r.receiver.id = :studentId))
            """)
    boolean existsPendingBetween(@Param("studentId") Long studentId,
                                 @Param("otherStudentId") Long otherStudentId);
}
