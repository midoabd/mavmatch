package com.mavmatch.repository;

import com.mavmatch.model.Match;
import com.mavmatch.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStudent1OrStudent2(Student student1, Student student2);

    @Query("SELECT m FROM Match m WHERE m.student1.id = :studentId OR m.student2.id = :studentId ORDER BY m.overlapHours DESC")
    List<Match> findByStudentIdOrderByOverlapHoursDesc(@Param("studentId") Long studentId);

    @Query("SELECT m FROM Match m WHERE (m.student1.id = :s1 AND m.student2.id = :s2) OR (m.student1.id = :s2 AND m.student2.id = :s1)")
    List<Match> findExistingMatch(@Param("s1") Long s1, @Param("s2") Long s2);

    void deleteByStudent1OrStudent2(Student student1, Student student2);
}