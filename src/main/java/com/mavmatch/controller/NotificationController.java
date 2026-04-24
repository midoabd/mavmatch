package com.mavmatch.controller;

import com.mavmatch.repository.MessageRepository;
import com.mavmatch.repository.MeetingRequestRepository;
import com.mavmatch.repository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired private MessageRepository messageRepo;
    @Autowired private MeetingRequestRepository meetingRepo;
    @Autowired private MatchRepository matchRepo;

    @GetMapping("/notifications")
    public List<Map<String, Object>> getNotifications(@RequestParam Long studentId) {
        List<Map<String, Object>> notifications = new ArrayList<>();

        // Pending meeting requests (received)
        meetingRepo.findAll().stream()
                .filter(m -> m.getReceiverId() != null &&
                        m.getReceiverId().equals(studentId) &&
                        m.getStatus() == com.mavmatch.model.MeetingRequest.Status.PENDING)
                .forEach(m -> {
                    Map<String, Object> n = new HashMap<>();
                    n.put("type", "meeting_request");
                    n.put("id", m.getId());
                    n.put("message", m.getRequester().getFirstName() + " " + m.getRequester().getLastName() +
                            " wants to study " + m.getCourseCode() + " with you");
                    n.put("time", m.getCreatedAt().toString());
                    n.put("meetingId", m.getId());
                    notifications.add(n);
                });

        // New messages from real users
        matchRepo.findByStudentIdOrderByOverlapHoursDesc(studentId).forEach(match -> {
            Long otherId = match.getStudent1().getId().equals(studentId)
                    ? match.getStudent2().getId() : match.getStudent1().getId();
            if(otherId <= 100) return; // skip bots

            long unread = messageRepo.findByMatchIdOrderBySentAtAsc(match.getId())
                    .stream()
                    .filter(msg -> !msg.getSender().getId().equals(studentId))
                    .count();

            if(unread > 0){
                String otherName = match.getStudent1().getId().equals(studentId)
                        ? match.getStudent2().getFirstName() + " " + match.getStudent2().getLastName()
                        : match.getStudent1().getFirstName() + " " + match.getStudent1().getLastName();
                Map<String, Object> n = new HashMap<>();
                n.put("type", "message");
                n.put("id", match.getId());
                n.put("message", otherName + " sent you " + unread + " message" + (unread > 1 ? "s" : ""));
                n.put("matchId", match.getId());
                notifications.add(n);
            }
        });

        return notifications;
    }
}