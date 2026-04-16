package com.mavmatch.controller;

import com.mavmatch.repository.StudentRepository;
import com.mavmatch.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private StudentRepository studentRepo;

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
        studentRepo.deleteById(id);
        return Map.of("success", true, "message", "Student " + id + " deleted");
    }
}