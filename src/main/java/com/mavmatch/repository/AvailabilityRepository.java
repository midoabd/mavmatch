package com.mavmatch.repository;

import com.mavmatch.model.Availability;
import com.mavmatch.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByStudent(Student student);
    List<Availability> findByStudentId(Long studentId);
    void deleteByStudent(Student student);
}