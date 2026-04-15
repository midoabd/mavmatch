package com.mavmatch.service;

import com.mavmatch.model.*;
import com.mavmatch.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private StudentCourseRepository studentCourseRepo;

    @Autowired
    private AvailabilityRepository availabilityRepo;

    @Autowired
    private BlockedUserRepository blockedUserRepo;

    @Autowired
    private CourseRepository courseRepo;

    public Map<String, Object> getProfile(Long studentId) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return Map.of("success", false, "message", "Student not found");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("studentId", student.getId());
        result.put("firstName", student.getFirstName());
        result.put("lastName", student.getLastName());
        result.put("email", student.getEmail());
        result.put("major", student.getMajor());
        result.put("utaId", student.getUtaId());
        return result;
    }

    public Map<String, Object> updateProfile(Long studentId, String firstName, String lastName, String major) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return Map.of("success", false, "message", "Student not found");

        if (firstName != null) student.setFirstName(firstName);
        if (lastName != null) student.setLastName(lastName);
        if (major != null) student.setMajor(major);
        studentRepo.save(student);

        return Map.of("success", true, "message", "Profile updated");
    }

    public List<Map<String, Object>> getCourses(Long studentId) {
        return studentCourseRepo.findByStudentId(studentId).stream().map(sc -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", sc.getId());
            m.put("courseCode", sc.getCourse().getCourseCode());
            m.put("courseName", sc.getCourse().getCourseName());
            m.put("scheduleDays", sc.getScheduleDays());
            m.put("scheduleTime", sc.getScheduleTime());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> addCourse(Long studentId, String courseCode, String courseName,
                                         String days, String time) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return Map.of("success", false, "message", "Student not found");

        Course course = courseRepo.findByCourseCode(courseCode).orElseGet(() -> {
            Course c = new Course();
            c.setCourseCode(courseCode);
            c.setCourseName(courseName != null ? courseName : courseCode);
            c.setDepartment("General");
            return courseRepo.save(c);
        });

        if (studentCourseRepo.existsByStudentAndCourse(student, course)) {
            return Map.of("success", false, "message", "Course already added");
        }

        StudentCourse sc = new StudentCourse();
        sc.setStudent(student);
        sc.setCourse(course);
        sc.setScheduleDays(days != null ? days : "TBD");
        sc.setScheduleTime(time != null ? time : "");
        studentCourseRepo.save(sc);

        return Map.of("success", true, "message", "Course added", "courseCode", courseCode);
    }

    @Transactional
    public Map<String, Object> removeCourse(Long studentId, String courseCode) {
        Student student = studentRepo.findById(studentId).orElse(null);
        Course course = courseRepo.findByCourseCode(courseCode).orElse(null);
        if (student == null || course == null) {
            return Map.of("success", false, "message", "Not found");
        }
        studentCourseRepo.deleteByStudentAndCourse(student, course);
        return Map.of("success", true, "message", "Course removed");
    }

    public List<Map<String, Object>> getAvailability(Long studentId) {
        return availabilityRepo.findByStudentId(studentId).stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("day", a.getDayOfWeek());
            m.put("startHour", a.getStartHour());
            m.put("endHour", a.getEndHour());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> saveAvailability(Long studentId, List<Map<String, Object>> slots) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return Map.of("success", false, "message", "Student not found");

        availabilityRepo.deleteByStudent(student);

        for (Map<String, Object> slot : slots) {
            Availability a = new Availability();
            a.setStudent(student);
            a.setDayOfWeek((String) slot.get("day"));
            a.setStartHour(((Number) slot.get("startHour")).intValue());
            a.setEndHour(((Number) slot.get("endHour")).intValue());
            availabilityRepo.save(a);
        }

        return Map.of("success", true, "message", "Availability saved", "count", slots.size());
    }

    @Transactional
    public Map<String, Object> blockUser(Long blockerId, Long blockedId) {
        Student blocker = studentRepo.findById(blockerId).orElse(null);
        Student blocked = studentRepo.findById(blockedId).orElse(null);
        if (blocker == null || blocked == null) {
            return Map.of("success", false, "message", "Student not found");
        }
        if (blockedUserRepo.existsByBlockerAndBlocked(blocker, blocked)) {
            return Map.of("success", false, "message", "Already blocked");
        }
        BlockedUser bu = new BlockedUser();
        bu.setBlocker(blocker);
        bu.setBlocked(blocked);
        blockedUserRepo.save(bu);
        return Map.of("success", true, "message", "User blocked");
    }

    @Transactional
    public Map<String, Object> unblockUser(Long blockerId, Long blockedId) {
        Student blocker = studentRepo.findById(blockerId).orElse(null);
        Student blocked = studentRepo.findById(blockedId).orElse(null);
        if (blocker == null || blocked == null) {
            return Map.of("success", false, "message", "Student not found");
        }
        blockedUserRepo.deleteByBlockerAndBlocked(blocker, blocked);
        return Map.of("success", true, "message", "User unblocked");
    }
}