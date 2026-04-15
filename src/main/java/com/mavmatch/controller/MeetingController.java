package com.mavmatch.controller;

import com.mavmatch.model.MeetingRequest;
import com.mavmatch.model.Student;
import com.mavmatch.repository.MeetingRequestRepository;
import com.mavmatch.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MeetingController {

    @Autowired private MeetingRequestRepository meetingRepo;
    @Autowired private StudentRepository studentRepo;

    @GetMapping("/meetings")
    public List<Map<String, Object>> getMeetings(@RequestParam Long studentId) {
        return meetingRepo.findByRequesterId(studentId).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("partner", m.getPartnerName());
            map.put("course", m.getCourseCode());
            map.put("date", m.getMeetingDate());
            map.put("day", m.getDayOfWeek());
            map.put("time", m.getMeetingTime());
            map.put("duration", m.getDuration());
            map.put("location", m.getLocation());
            map.put("notes", m.getMessage());
            map.put("status", m.getStatus().toString().toLowerCase());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/meetings")
    public Map<String, Object> createMeeting(@RequestBody Map<String, Object> body) {
        Long studentId = ((Number) body.get("studentId")).longValue();
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return Map.of("success", false, "message", "Student not found");

        MeetingRequest meeting = new MeetingRequest();
        meeting.setRequester(student);
        meeting.setPartnerName((String) body.getOrDefault("partner", ""));
        meeting.setCourseCode((String) body.getOrDefault("course", ""));
        meeting.setMeetingDate((String) body.getOrDefault("date", ""));
        meeting.setDayOfWeek((String) body.getOrDefault("day", ""));
        meeting.setMeetingTime((String) body.getOrDefault("time", ""));
        meeting.setDuration((String) body.getOrDefault("duration", "1 hour"));
        meeting.setLocation((String) body.getOrDefault("location", ""));
        meeting.setMessage((String) body.getOrDefault("notes", ""));
        meeting.setStatus(MeetingRequest.Status.CONFIRMED);
        meeting = meetingRepo.save(meeting);

        return Map.of("success", true, "id", meeting.getId());
    }

    @DeleteMapping("/meetings/{id}")
    public Map<String, Object> deleteMeeting(@PathVariable Long id) {
        meetingRepo.deleteById(id);
        return Map.of("success", true);
    }
}