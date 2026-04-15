package com.mavmatch.service;

import com.mavmatch.model.*;
import com.mavmatch.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private MatchRepository matchRepo;

    @Autowired
    private StudentRepository studentRepo;

    public List<Map<String, Object>> getMessages(Long matchId) {
        List<Message> messages = messageRepo.findByMatchIdOrderBySentAtAsc(matchId);

        // If no messages exist, return demo messages
        if (messages.isEmpty()) {
            return getDemoMessages(matchId);
        }

        return messages.stream().map(this::messageToMap).collect(Collectors.toList());
    }

    public Map<String, Object> sendMessage(Long matchId, Long senderId, String content) {
        Match match = matchRepo.findById(matchId).orElse(null);
        Student sender = studentRepo.findById(senderId).orElse(null);

        if (match == null || sender == null) {
            return Map.of("success", false, "message", "Invalid match or sender");
        }

        Message message = new Message();
        message.setMatch(match);
        message.setSender(sender);
        message.setContent(content);
        message.setReported(false);
        message = messageRepo.save(message);

        Map<String, Object> result = messageToMap(message);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> reportMessage(Long messageId, String reason) {
        Message message = messageRepo.findById(messageId).orElse(null);
        if (message == null) {
            return Map.of("success", false, "message", "Message not found");
        }
        message.setReported(true);
        message.setReportReason(reason);
        messageRepo.save(message);
        return Map.of("success", true, "message", "Message reported");
    }

    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", message.getId());
        m.put("matchId", message.getMatch().getId());
        m.put("senderId", message.getSender().getId());
        m.put("senderName", message.getSender().getFirstName());
        m.put("content", message.getContent());
        m.put("sentAt", message.getSentAt().toString());
        m.put("isReported", message.isReported());
        return m;
    }

    private List<Map<String, Object>> getDemoMessages(Long matchId) {
        List<Map<String, Object>> demo = new ArrayList<>();
        Match match = matchRepo.findById(matchId).orElse(null);
        if (match == null) return demo;

        String[][] demoMsgs = {
                {"Hey! I saw we're both in the same class. Want to study together?", "student2"},
                {"Hey! Yeah definitely, I've been looking for a study partner!", "student1"},
                {"Great! I'm free Monday and Wednesday afternoons. How about you?", "student2"},
                {"Monday works great for me! Should we meet at the library?", "student1"},
                {"The library on the 3rd floor has good study rooms. Let's do it!", "student2"},
                {"Perfect! See you Monday at 2pm then 📚", "student1"}
        };

        for (int i = 0; i < demoMsgs.length; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", -(i + 1));
            m.put("matchId", matchId);
            boolean isStudent1 = demoMsgs[i][1].equals("student1");
            Student sender = isStudent1 ? match.getStudent1() : match.getStudent2();
            m.put("senderId", sender.getId());
            m.put("senderName", sender.getFirstName());
            m.put("content", demoMsgs[i][0]);
            m.put("sentAt", "2026-04-10T1" + i + ":00:00");
            m.put("isReported", false);
            m.put("isDemo", true);
            demo.add(m);
        }
        return demo;
    }
}