package com.mavmatch.repository;

import com.mavmatch.model.StudentCourse;
import com.mavmatch.model.Student;
import com.mavmatch.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {
    List<StudentCourse> findByStudent(Student student);
    List<StudentCourse> findByCourse(Course course);
    List<StudentCourse> findByStudentId(Long studentId);
    boolean existsByStudentAndCourse(Student student, Course course);
    void deleteByStudentAndCourse(Student student, Course course);
}