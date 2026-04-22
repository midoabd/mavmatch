package com.mavmatch.controller;

import com.mavmatch.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MatchController {

    @Autowired
    private MatchingService matchingService;

    @GetMapping("/matches")
    public List<Map<String, Object>> getMatches(
            @RequestParam Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean refresh) {
        if (refresh) {
            return matchingService.recomputeMatches(studentId, page);
        }
        return matchingService.findMatches(studentId, page);
    }
}