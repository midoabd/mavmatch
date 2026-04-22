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
        // Get meetings where student is requester OR receiver
        List<MeetingRequest> all = meetingRepo.findAll();
        return all.stream()
                .filter(m -> {
                    boolean isRequester = m.getRequester().getId().equals(studentId);
                    boolean isReceiver = m.getReceiverId() != null && m.getReceiverId().equals(studentId);
                    return isRequester || isReceiver;
                })
                .map(m -> toMap(m, studentId))
                .collect(Collectors.toList());
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

        // Try to find receiver by name
        Long receiverId = body.get("receiverId") != null ? ((Number) body.get("receiverId")).longValue() : null;
        if (receiverId != null) {
            meeting.setReceiverId(receiverId);
            meeting.setStatus(MeetingRequest.Status.PENDING);
        } else {
            meeting.setStatus(MeetingRequest.Status.CONFIRMED);
        }

        meeting = meetingRepo.save(meeting);
        return Map.of("success", true, "id", meeting.getId());
    }

    @PutMapping("/meetings/{id}")
    public Map<String, Object> updateMeeting(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        MeetingRequest meeting = meetingRepo.findById(id).orElse(null);
        if (meeting == null) return Map.of("success", false, "message", "Not found");
        String status = (String) body.get("status");
        meeting.setStatus(MeetingRequest.Status.valueOf(status.toUpperCase()));
        meetingRepo.save(meeting);
        return Map.of("success", true);
    }

    @DeleteMapping("/meetings/{id}")
    public Map<String, Object> deleteMeeting(@PathVariable Long id) {
        meetingRepo.deleteById(id);
        return Map.of("success", true);
    }

    private Map<String, Object> toMap(MeetingRequest m, Long viewerId) {
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
        map.put("isRequester", m.getRequester().getId().equals(viewerId));
        map.put("receiverId", m.getReceiverId());
        return map;
    }
}