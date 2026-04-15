package com.mavmatch.controller;

import com.mavmatch.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageService messageService;

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
}