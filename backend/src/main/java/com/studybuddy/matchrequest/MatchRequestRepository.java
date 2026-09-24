package com.studybuddy.matchrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    List<MatchRequest> findBySenderId(Long senderId);

    List<MatchRequest> findByReceiverId(Long receiverId);
}
