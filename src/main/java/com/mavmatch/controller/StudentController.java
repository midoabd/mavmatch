package com.mavmatch.controller;

import com.mavmatch.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestParam Long studentId) {
        return studentService.getProfile(studentId);
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body) {
        Long studentId = ((Number) body.get("studentId")).longValue();
        return studentService.updateProfile(
                studentId,
                (String) body.get("firstName"),
                (String) body.get("lastName"),
                (String) body.get("major")
        );
    }

    @GetMapping("/courses")
    public List<Map<String, Object>> getCourses(@RequestParam Long studentId) {
        return studentService.getCourses(studentId);
    }

    @PostMapping("/courses")
    public Map<String, Object> addCourse(@RequestBody Map<String, Object> body) {
        Long studentId = ((Number) body.get("studentId")).longValue();
        return studentService.addCourse(
                studentId,
                (String) body.get("courseCode"),
                (String) body.get("courseName"),
                (String) body.get("days"),
                (String) body.get("time")
        );
    }

    @DeleteMapping("/courses")
    public Map<String, Object> removeCourse(@RequestParam Long studentId,
                                            @RequestParam String courseCode) {
        return studentService.removeCourse(studentId, courseCode);
    }

    @GetMapping("/availability")
    public List<Map<String, Object>> getAvailability(@RequestParam Long studentId) {
        return studentService.getAvailability(studentId);
    }

    @PostMapping("/availability")
    public Map<String, Object> saveAvailability(@RequestBody Map<String, Object> body) {
        Long studentId = ((Number) body.get("studentId")).longValue();
        List<Map<String, Object>> slots = (List<Map<String, Object>>) body.get("slots");
        return studentService.saveAvailability(studentId, slots);
    }

    @PostMapping("/block")
    public Map<String, Object> blockUser(@RequestBody Map<String, Object> body) {
        Long blockerId = ((Number) body.get("blockerId")).longValue();
        Long blockedId = ((Number) body.get("blockedId")).longValue();
        return studentService.blockUser(blockerId, blockedId);
    }

    @PostMapping("/unblock")
    public Map<String, Object> unblockUser(@RequestBody Map<String, Object> body) {
        Long blockerId = ((Number) body.get("blockerId")).longValue();
        Long blockedId = ((Number) body.get("blockedId")).longValue();
        return studentService.unblockUser(blockerId, blockedId);
    }
}