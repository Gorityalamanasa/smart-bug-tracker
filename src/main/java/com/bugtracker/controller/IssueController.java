package com.bugtracker.controller;

import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Role;
import com.bugtracker.model.enums.Status;
import com.bugtracker.service.IssueService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST controller for issue management operations.
 * Includes Role-Based Access Control (RBAC) via X-Acting-User-Id header.
 *
 * Permissions:
 *   ADMIN     — Full access to all operations
 *   DEVELOPER — Edit (own/assigned), change status (assigned), no delete, no assign
 *   TESTER    — Change status (verify/reopen only), no edit, no delete, no assign
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

    /** POST /api/issues — Create a new issue (all roles can create) */
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

    /** PUT /api/issues/{id} — Update an issue (ADMIN or DEVELOPER who is assignee/reporter) */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateIssue(@PathVariable Long id,
                                         @Valid @RequestBody Issue issueDetails,
                                         @RequestHeader(value = "X-Acting-User-Id", required = false) Long actingUserId) {
        // RBAC check
        if (actingUserId != null) {
            User actingUser = userService.getUserById(actingUserId).orElse(null);
            if (actingUser != null) {
                Role role = actingUser.getRole();
                if (role == Role.TESTER) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "TESTER role cannot edit issues"));
                }
                if (role == Role.DEVELOPER) {
                    Issue existing = issueService.getIssueById(id).orElse(null);
                    if (existing != null) {
                        boolean isAssignee = existing.getAssignee() != null && existing.getAssignee().getId().equals(actingUserId);
                        boolean isReporter = existing.getReporter() != null && existing.getReporter().getId().equals(actingUserId);
                        if (!isAssignee && !isReporter) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "DEVELOPER can only edit issues they reported or are assigned to"));
                        }
                    }
                }
            }
        }
        try {
            Issue updated = issueService.updateIssue(id, issueDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PATCH /api/issues/{id}/status — Change issue status (role-restricted) */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-Acting-User-Id", required = false) Long actingUserId) {
        try {
            Status newStatus = Status.valueOf(body.get("status"));

            // RBAC check for TESTER — can only verify/reopen
            if (actingUserId != null) {
                User actingUser = userService.getUserById(actingUserId).orElse(null);
                Issue existing = issueService.getIssueById(id).orElse(null);
                if (actingUser != null && existing != null) {
                    Role role = actingUser.getRole();
                    if (role == Role.TESTER) {
                        // Testers can ONLY: RESOLVED→CLOSED, RESOLVED→OPEN, CLOSED→OPEN
                        Set<String> allowed = Set.of("RESOLVED->CLOSED", "RESOLVED->OPEN", "CLOSED->OPEN");
                        String transition = existing.getStatus() + "->" + newStatus;
                        if (!allowed.contains(transition)) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error",
                                        "TESTER can only verify (RESOLVED→CLOSED), reopen (RESOLVED→OPEN), or reopen (CLOSED→OPEN)"));
                        }
                    }
                    if (role == Role.DEVELOPER) {
                        // Developers can only change status on issues assigned to them
                        boolean isAssignee = existing.getAssignee() != null && existing.getAssignee().getId().equals(actingUserId);
                        if (!isAssignee) {
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(Map.of("error", "DEVELOPER can only change status on issues assigned to them"));
                        }
                    }
                }
            }

            Issue updated = issueService.changeStatus(id, newStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** PATCH /api/issues/{id}/assign — Assign issue to a user (ADMIN only) */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignIssue(@PathVariable Long id,
                                         @RequestBody Map<String, Long> body,
                                         @RequestHeader(value = "X-Acting-User-Id", required = false) Long actingUserId) {
        // RBAC check — only ADMIN can assign
        if (actingUserId != null) {
            User actingUser = userService.getUserById(actingUserId).orElse(null);
            if (actingUser != null && actingUser.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only ADMIN can assign issues"));
            }
        }
        try {
            Long assigneeId = body.get("assigneeId");
            Issue updated = issueService.assignIssue(id, assigneeId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** DELETE /api/issues/{id} — Delete an issue (ADMIN only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIssue(@PathVariable Long id,
                                         @RequestHeader(value = "X-Acting-User-Id", required = false) Long actingUserId) {
        // RBAC check — only ADMIN can delete
        if (actingUserId != null) {
            User actingUser = userService.getUserById(actingUserId).orElse(null);
            if (actingUser != null && actingUser.getRole() != Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Only ADMIN can delete issues"));
            }
        }
        try {
            issueService.deleteIssue(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
