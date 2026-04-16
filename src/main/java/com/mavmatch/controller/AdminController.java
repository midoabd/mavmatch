package com.mavmatch.controller;

import com.mavmatch.repository.*;
import com.mavmatch.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            // Delete all related records first
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
}