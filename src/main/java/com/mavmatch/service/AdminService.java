package com.mavmatch.service;

import com.mavmatch.model.*;
import com.mavmatch.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private MeetingRequestRepository meetingRepo;

    @Autowired
    private BlockedUserRepository blockedUserRepo;

    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", studentRepo.count());
        stats.put("totalMeetings", meetingRepo.count());
        stats.put("reportedMessages", messageRepo.countByIsReportedTrue());
        dashboard.put("stats", stats);

        // Recent students
        List<Map<String, Object>> students = studentRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(20)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("name", s.getFirstName() + " " + s.getLastName());
                    m.put("email", s.getEmail());
                    m.put("major", s.getMajor());
                    m.put("createdAt", s.getCreatedAt().toString());
                    return m;
                }).collect(Collectors.toList());
        dashboard.put("recentStudents", students);

        // Recent matches
        List<Map<String, Object>> matches = matchRepo.findAll().stream()
                .sorted((a, b) -> b.getMatchedAt().compareTo(a.getMatchedAt()))
                .limit(20)
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("student1", m.getStudent1().getFirstName() + " " + m.getStudent1().getLastName());
                    map.put("student2", m.getStudent2().getFirstName() + " " + m.getStudent2().getLastName());
                    map.put("course", m.getCourse() != null ? m.getCourse().getCourseCode() : "N/A");
                    map.put("overlapHours", m.getOverlapHours());
                    map.put("matchedAt", m.getMatchedAt().toString());
                    return map;
                }).collect(Collectors.toList());
        dashboard.put("recentMatches", matches);

        // Reported messages
        List<Map<String, Object>> reported = messageRepo.findByIsReportedTrue().stream()
                .map(msg -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", msg.getId());
                    m.put("senderId", msg.getSender() != null ? msg.getSender().getId() : null);
                    m.put("sender", msg.getSender() != null ? msg.getSender().getFirstName() + " " + msg.getSender().getLastName() : "Unknown");
                    m.put("senderEmail", msg.getSender() != null ? msg.getSender().getEmail() : "");
                    m.put("content", msg.getContent());
                    m.put("reason", msg.getReportReason());
                    m.put("sentAt", msg.getSentAt() != null ? msg.getSentAt().toString() : "");
                    return m;
                }).collect(Collectors.toList());
        dashboard.put("reportedMessages", reported);

        // Recent meetings
        List<Map<String, Object>> meetings = meetingRepo.findAllByOrderByCreatedAtDesc().stream()
                .limit(20)
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getId());
                    map.put("requester", m.getRequester().getFirstName() + " " + m.getRequester().getLastName());
                    map.put("day", m.getDayOfWeek());
                    map.put("time", m.getMeetingTime());
                    map.put("location", m.getLocation());
                    map.put("status", m.getStatus().toString());
                    map.put("createdAt", m.getCreatedAt().toString());
                    return map;
                }).collect(Collectors.toList());
        dashboard.put("recentMeetings", meetings);

        return dashboard;
    }

    public List<Map<String, Object>> getAllStudents() {
        return studentRepo.findAll().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getFirstName() + " " + s.getLastName());
            m.put("email", s.getEmail());
            m.put("major", s.getMajor());
            m.put("utaId", s.getUtaId());
            m.put("createdAt", s.getCreatedAt().toString());
            return m;
        }).collect(Collectors.toList());
    }
}