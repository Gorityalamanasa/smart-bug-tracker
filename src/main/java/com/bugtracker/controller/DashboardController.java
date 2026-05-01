package com.bugtracker.controller;

import com.bugtracker.service.IssueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * REST controller for dashboard statistics and health check.
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final IssueService issueService;

    public DashboardController(IssueService issueService) {
        this.issueService = issueService;
    }

    /** GET /api/dashboard/stats — Get dashboard statistics */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(issueService.getDashboardStats());
    }

    /** GET /api/health — Health check endpoint */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "application", "Smart Bug Tracker",
            "version", "1.0.0"
        ));
    }
}
