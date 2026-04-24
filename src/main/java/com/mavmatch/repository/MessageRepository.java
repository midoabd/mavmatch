package com.mavmatch.repository;

import com.mavmatch.model.Message;
import com.mavmatch.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByMatchOrderBySentAtAsc(Match match);
    List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);
    List<Message> findByIsReportedTrue();
    List<Message> findBySenderId(Long senderId);
    long countByIsReportedTrue();
}