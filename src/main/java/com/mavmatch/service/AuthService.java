package com.mavmatch.service;

import com.mavmatch.model.Student;
import com.mavmatch.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

        // Check if banned
        if (student.getBannedUntil() != null && student.getBannedUntil().isAfter(LocalDateTime.now())) {
            LocalDateTime bannedUntil = student.getBannedUntil();
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), bannedUntil);
            long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), bannedUntil) % 24;
            String timeLeft = daysLeft > 0 ? daysLeft + " day" + (daysLeft != 1 ? "s" : "") + " and " + hoursLeft + " hour" + (hoursLeft != 1 ? "s" : "") : hoursLeft + " hour" + (hoursLeft != 1 ? "s" : "");
            result.put("success", false);
            result.put("message", "Your account has been suspended. Ban lifts in " + timeLeft + ".");
            result.put("banned", true);
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