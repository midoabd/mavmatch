package com.mavmatch.repository;

import com.mavmatch.model.BlockedUser;
import com.mavmatch.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockedUser, Long> {
    List<BlockedUser> findByBlocker(Student blocker);
    List<BlockedUser> findByBlockerId(Long blockerId);
    boolean existsByBlockerAndBlocked(Student blocker, Student blocked);
    void deleteByBlockerAndBlocked(Student blocker, Student blocked);
}