package com.mavmatch.repository;

import com.mavmatch.model.MeetingRequest;
import com.mavmatch.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MeetingRequestRepository extends JpaRepository<MeetingRequest, Long> {
    List<MeetingRequest> findByRequesterId(Long studentId);
    List<MeetingRequest> findByRequester(Student requester);
    List<MeetingRequest> findAllByOrderByCreatedAtDesc();
}