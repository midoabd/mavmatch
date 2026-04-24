package com.mavmatch.controller;

import com.mavmatch.model.Student;
import com.mavmatch.repository.*;
import com.mavmatch.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private AdminService adminService;
    @Autowired private StudentRepository studentRepo;
    @Autowired private StudentCourseRepository studentCourseRepo;
    @Autowired private AvailabilityRepository availabilityRepo;
    @Autowired private MatchRepository matchRepo;
    @Autowired private MeetingRequestRepository meetingRepo;
    @Autowired private BlockedUserRepository blockedUserRepo;
    @Autowired private MessageRepository messageRepo;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/students")
    public List<Map<String, Object>> getAllStudents() {
        return adminService.getAllStudents();
    }

    @DeleteMapping("/students/{id}")
    public Map<String, Object> deleteStudent(@PathVariable Long id) {
        try {
            messageRepo.deleteAll(messageRepo.findBySenderId(id));
            studentCourseRepo.deleteAll(studentCourseRepo.findByStudentId(id));
            availabilityRepo.deleteAll(availabilityRepo.findByStudentId(id));
            matchRepo.deleteAll(matchRepo.findByStudentIdOrderByOverlapHoursDesc(id));
            meetingRepo.deleteAll(meetingRepo.findByRequesterId(id));
            blockedUserRepo.deleteAll(blockedUserRepo.findByBlockerId(id));
            studentRepo.deleteById(id);
            return Map.of("success", true, "message", "Student " + id + " deleted");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/students/{id}/ban")
    public Map<String, Object> banStudent(@PathVariable Long id) {
        try {
            Student student = studentRepo.findById(id).orElse(null);
            if(student == null) return Map.of("success", false, "message", "Student not found");
            student.setBannedUntil(LocalDateTime.now().plusWeeks(1));
            studentRepo.save(student);
            return Map.of("success", true, "message", "Student banned for 1 week");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @DeleteMapping("/messages/{id}")
    public Map<String, Object> deleteMessage(@PathVariable Long id) {
        messageRepo.deleteById(id);
        return Map.of("success", true);
    }
}