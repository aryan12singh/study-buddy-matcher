package com.studybuddy.matchrequest;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MatchRequest r where r.id = :id")
    Optional<MatchRequest> findByIdForUpdate(@Param("id") Long id);
    interface Participants {
        Long getSenderId();
        Long getReceiverId();
    }

    @Query("select r.sender.id as senderId, r.receiver.id as receiverId from MatchRequest r where r.id = :id")
    Optional<Participants> findParticipants(@Param("id") Long id);

    interface RequestParticipants extends Participants {
        Long getId();
    }

    @Query("select r.id as id, r.sender.id as senderId, r.receiver.id as receiverId from MatchRequest r where r.id in :ids")
    List<RequestParticipants> findParticipantsByIds(@Param("ids") Set<Long> ids);

    @Query("select r from MatchRequest r where r.status=com.studybuddy.matchrequest.MatchRequestStatus.PENDING and ((r.sender.id=:first and r.receiver.id=:second) or (r.sender.id=:second and r.receiver.id=:first))")
    Optional<MatchRequest> findPendingBetween(@Param("first") Long first, @Param("second") Long second);
    long countByReceiverIdAndStatus(Long id, MatchRequestStatus status);
    long countBySenderIdAndStatus(Long id, MatchRequestStatus status);
    List<MatchRequest> findBySenderId(Long senderId);
    List<MatchRequest> findByReceiverId(Long receiverId);
    List<MatchRequest> findBySenderIdOrderByCreatedAtDesc(Long senderId);
    List<MatchRequest> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    List<MatchRequest> findBySenderIdAndStatus(Long senderId, MatchRequestStatus status);
    List<MatchRequest> findByReceiverIdAndStatus(Long receiverId, MatchRequestStatus status);

    /** Every request the student has sent, in any status. */
    long countBySenderId(Long senderId);
}
