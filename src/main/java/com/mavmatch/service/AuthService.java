package com.mavmatch.service;

import com.mavmatch.model.Student;
import com.mavmatch.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> register(String firstName, String lastName, String email,
                                        String password, String utaId, String major) {
        Map<String, Object> result = new HashMap<>();

        if (studentRepo.existsByEmail(email)) {
            result.put("success", false);
            result.put("message", "Email already registered");
            return result;
        }

        if (utaId != null && !utaId.isEmpty() && studentRepo.existsByUtaId(utaId)) {
            result.put("success", false);
            result.put("message", "UTA ID already registered");
            return result;
        }

        Student student = new Student();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPasswordHash(passwordEncoder.encode(password));
        student.setUtaId(utaId);
        student.setMajor(major);
        student.setActive(true);

        student = studentRepo.save(student);

        result.put("success", true);
        result.put("message", "Registration successful");
        result.put("studentId", student.getId());
        result.put("firstName", student.getFirstName());
        result.put("lastName", student.getLastName());
        result.put("email", student.getEmail());
        result.put("major", student.getMajor());
        result.put("utaId", student.getUtaId());
        return result;
    }

    public Map<String, Object> login(String email, String password) {
        Map<String, Object> result = new HashMap<>();

        Optional<Student> studentOpt = studentRepo.findByEmail(email);
        if (studentOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Invalid email or password");
            return result;
        }

        Student student = studentOpt.get();
        if (!passwordEncoder.matches(password, student.getPasswordHash())) {
            result.put("success", false);
            result.put("message", "Invalid email or password");
            return result;
        }

        result.put("success", true);
        result.put("studentId", student.getId());
        result.put("firstName", student.getFirstName());
        result.put("lastName", student.getLastName());
        result.put("email", student.getEmail());
        result.put("major", student.getMajor());
        result.put("utaId", student.getUtaId());
        return result;
    }
}