package com.mavmatch.controller;

import com.mavmatch.model.Message;
import com.mavmatch.model.Match;
import com.mavmatch.model.Student;
import com.mavmatch.repository.MessageRepository;
import com.mavmatch.repository.MatchRepository;
import com.mavmatch.repository.StudentRepository;
import com.mavmatch.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired private MessageService messageService;
    @Autowired private MessageRepository messageRepo;
    @Autowired private MatchRepository matchRepo;
    @Autowired private StudentRepository studentRepo;

    @GetMapping("/messages")
    public List<Map<String, Object>> getMessages(@RequestParam Long matchId) {
        return messageService.getMessages(matchId);
    }

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> body) {
        Long matchId = ((Number) body.get("matchId")).longValue();
        Long senderId = ((Number) body.get("senderId")).longValue();
        String content = (String) body.get("content");
        return messageService.sendMessage(matchId, senderId, content);
    }

    @PostMapping("/messages/report")
    public Map<String, Object> reportMessage(@RequestBody Map<String, Object> body) {
        Long messageId = ((Number) body.get("messageId")).longValue();
        String reason = (String) body.get("reason");
        return messageService.reportMessage(messageId, reason);
    }

    @PostMapping("/messages/report-content")
    public Map<String, Object> reportByContent(@RequestBody Map<String, Object> body) {
        String content = (String) body.getOrDefault("content", "");
        String senderName = (String) body.getOrDefault("senderName", "Unknown");
        String reason = (String) body.getOrDefault("reason", "");
        Long reporterId = body.get("reporterId") != null ? ((Number) body.get("reporterId")).longValue() : null;

        // Create a fake message record for the report
        Message msg = new Message();
        msg.setContent("[Reported bot message from " + senderName + "]: " + content);
        msg.setReported(true);
        msg.setReportReason(reason);
        // Use reporter as sender placeholder
        if(reporterId != null){
            studentRepo.findById(reporterId).ifPresent(msg::setSender);
        }
        // Need a match - find any match for this reporter
        if(reporterId != null){
            matchRepo.findByStudentIdOrderByOverlapHoursDesc(reporterId).stream()
                    .findFirst().ifPresent(msg::setMatch);
        }
        if(msg.getSender() != null && msg.getMatch() != null){
            messageRepo.save(msg);
        }
        return Map.of("success", true);
    }
}