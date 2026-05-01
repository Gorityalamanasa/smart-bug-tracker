package com.bugtracker.controller;

import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import com.bugtracker.service.IssueService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * REST controller for issue management operations.
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final UserService userService;

    public IssueController(IssueService issueService, UserService userService) {
        this.issueService = issueService;
        this.userService = userService;
    }

    /** GET /api/issues — List all issues with optional filters */
    @GetMapping
    public ResponseEntity<List<Issue>> getAllIssues(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId) {

        List<Issue> issues;
        if (status != null) {
            issues = issueService.getIssuesByStatus(status);
        } else if (priority != null) {
            issues = issueService.getIssuesByPriority(priority);
        } else if (assigneeId != null) {
            issues = issueService.getIssuesByAssignee(assigneeId);
        } else {
            issues = issueService.getAllIssues();
        }
        return ResponseEntity.ok(issues);
    }

    /** GET /api/issues/{id} — Get issue by ID */
    @GetMapping("/{id}")
    public ResponseEntity<Issue> getIssueById(@PathVariable Long id) {
        return issueService.getIssueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/issues — Create a new issue */
    @PostMapping
    public ResponseEntity<Issue> createIssue(@Valid @RequestBody Issue issue,
                                             @RequestParam(required = false) Long reporterId) {
        if (reporterId != null) {
            User reporter = userService.getUserById(reporterId)
                    .orElseThrow(() -> new RuntimeException("Reporter not found"));
            issue.setReporter(reporter);
        }
        Issue created = issueService.createIssue(issue);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/issues/{id} — Update an issue */
    @PutMapping("/{id}")
    public ResponseEntity<Issue> updateIssue(@PathVariable Long id, @Valid @RequestBody Issue issueDetails) {
        try {
            Issue updated = issueService.updateIssue(id, issueDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PATCH /api/issues/{id}/status — Change issue status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            Status newStatus = Status.valueOf(body.get("status"));
            Issue updated = issueService.changeStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PATCH /api/issues/{id}/assign — Assign issue to a user */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignIssue(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long assigneeId = body.get("assigneeId");
            Issue updated = issueService.assignIssue(id, assigneeId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/issues/{id} — Delete an issue */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        try {
            issueService.deleteIssue(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
