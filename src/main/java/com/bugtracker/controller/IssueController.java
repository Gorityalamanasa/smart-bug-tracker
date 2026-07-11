package com.bugtracker.controller;

import com.bugtracker.dto.IssueCreateRequest;
import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Role;
import com.bugtracker.model.enums.Status;
import com.bugtracker.security.CustomUserDetails;
import com.bugtracker.service.IssueService;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST controller for issue management operations.
 * Uses JWT-based authentication — user is identified from SecurityContext.
 *
 * Role permissions:
 *   ADMIN     — View all issues, assign, manage, accept/reject AI triage, mark duplicates
 *   DEVELOPER — View assigned issues, update status, add comments (no AI duplicate data)
 *   TESTER    — Create issues (triggers AI triage), view own issues, verify/reopen (no AI data)
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

    // ── Helper: Get current authenticated user ──────────────────────

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    // ── Issue Listing (Role-Filtered) ───────────────────────────────

    /**
     * GET /api/issues — List issues based on role.
     *   ADMIN:     all issues (full AI data)
     *   DEVELOPER: only assigned issues (no duplicate analysis)
     *   TESTER:    only self-reported issues (no AI data, but sees duplicate status)
     */
    @GetMapping
    public ResponseEntity<List<Issue>> getIssues(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long assigneeId) {

        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        List<Issue> issues;

        if (role == Role.TESTER) {
            // Testers can only see their own issues
            issues = issueService.getIssuesByReporter(currentUser.getId());
        } else if (role == Role.DEVELOPER) {
            // Developers can only see issues assigned to them
            issues = issueService.getIssuesByAssignee(currentUser.getId());
        } else {
            // ADMIN sees all issues, with optional filters
            if (status != null) {
                issues = issueService.getIssuesByStatus(status);
            } else if (priority != null) {
                issues = issueService.getIssuesByPriority(priority);
            } else if (assigneeId != null) {
                issues = issueService.getIssuesByAssignee(assigneeId);
            } else {
                issues = issueService.getAllIssues();
            }
        }

        // Strip AI data based on role
        if (role == Role.TESTER) {
            issues.forEach(this::stripAllAiFields);
        } else if (role == Role.DEVELOPER) {
            issues.forEach(this::stripDuplicateFieldsForDeveloper);
        }

        return ResponseEntity.ok(issues);
    }

    /**
     * GET /api/issues/{id} — Get issue by ID.
     * Enforces visibility and strips AI data based on role.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getIssueById(@PathVariable Long id) {
        User currentUser = getCurrentUser();

        Optional<Issue> optIssue = issueService.getIssueById(id);
        if (optIssue.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Issue issue = optIssue.get();

        // Testers can only view their own issues
        if (currentUser.getRole() == Role.TESTER) {
            if (issue.getReporter() == null || !issue.getReporter().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only view issues you reported"));
            }
            stripAllAiFields(issue);
        }

        // Developers can only view assigned issues (or reported)
        if (currentUser.getRole() == Role.DEVELOPER) {
            if (issue.getAssignee() == null || !issue.getAssignee().getId().equals(currentUser.getId())) {
                if (issue.getReporter() == null || !issue.getReporter().getId().equals(currentUser.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "You can only view issues assigned to you"));
                }
            }
            stripDuplicateFieldsForDeveloper(issue);
        }

        return ResponseEntity.ok(issue);
    }

    // ── Issue Creation (with AI Triage) ─────────────────────────────

    /**
     * POST /api/issues — Create a new issue.
     * Reporter is automatically set from JWT.
     * AI triage is triggered to analyze the issue.
     */
    @PostMapping
    public ResponseEntity<?> createIssue(@Valid @RequestBody IssueCreateRequest request) {
        User currentUser = getCurrentUser();

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());

        // Create with AI triage analysis
        Issue created = issueService.createIssueWithTriage(issue, currentUser);

        if (currentUser.getRole() == Role.TESTER) {
            stripAllAiFields(created);
        } else if (currentUser.getRole() == Role.DEVELOPER) {
            stripDuplicateFieldsForDeveloper(created);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Issue Update ────────────────────────────────────────────────

    /**
     * PUT /api/issues/{id} — Update an issue.
     * ADMIN: full access. DEVELOPER: only assigned/reported. TESTER: forbidden.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateIssue(@PathVariable Long id,
                                          @Valid @RequestBody Issue issueDetails) {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        if (role == Role.TESTER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "TESTER role cannot edit issues"));
        }

        if (role == Role.DEVELOPER) {
            Issue existing = issueService.getIssueById(id).orElse(null);
            if (existing != null) {
                boolean isAssignee = existing.getAssignee() != null &&
                        existing.getAssignee().getId().equals(currentUser.getId());
                boolean isReporter = existing.getReporter() != null &&
                        existing.getReporter().getId().equals(currentUser.getId());
                if (!isAssignee && !isReporter) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "DEVELOPER can only edit issues they reported or are assigned to"));
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

    // ── Status Change ───────────────────────────────────────────────

    /**
     * PATCH /api/issues/{id}/status — Change issue status.
     * TESTER: can only verify (RESOLVED→CLOSED) or reopen (RESOLVED→REOPENED).
     * DEVELOPER: can only change status on assigned issues.
     * ADMIN: full access.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        try {
            Status newStatus = Status.valueOf(body.get("status"));
            User currentUser = getCurrentUser();
            Role role = currentUser.getRole();

            Issue existing = issueService.getIssueById(id).orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();

            if (role == Role.TESTER) {
                // Testers can only: RESOLVED→CLOSED, RESOLVED→REOPENED, DUPLICATE→REOPENED
                Set<String> allowed = Set.of("RESOLVED->CLOSED", "RESOLVED->REOPENED", "DUPLICATE->REOPENED");
                String transition = existing.getStatus() + "->" + newStatus;
                if (!allowed.contains(transition)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error",
                                "TESTER can only verify (RESOLVED→CLOSED), reopen (RESOLVED→REOPENED), or reopen a duplicate (DUPLICATE→REOPENED)"));
                }
                // Must be the reporter
                if (existing.getReporter() == null ||
                        !existing.getReporter().getId().equals(currentUser.getId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Only the original reporter can verify/reopen an issue"));
                }
            }

            if (role == Role.DEVELOPER) {
                boolean isAssignee = existing.getAssignee() != null &&
                        existing.getAssignee().getId().equals(currentUser.getId());
                if (!isAssignee) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "DEVELOPER can only change status on issues assigned to them"));
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

    // ── Issue Assignment (ADMIN only) ───────────────────────────────

    /**
     * PATCH /api/issues/{id}/assign — Assign issue to a developer. ADMIN only.
     */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignIssue(@PathVariable Long id,
                                          @RequestBody Map<String, Long> body) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can assign issues"));
        }

        try {
            Long assigneeId = body.get("assigneeId");
            Issue updated = issueService.assignIssue(id, assigneeId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── AI Triage Review (ADMIN only) ───────────────────────────────

    /**
     * PATCH /api/issues/{id}/accept-triage — Admin accepts AI suggestions.
     */
    @PatchMapping("/{id}/accept-triage")
    public ResponseEntity<?> acceptAiTriage(@PathVariable Long id,
                                             @RequestBody Map<String, Boolean> body) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can accept/reject AI triage suggestions"));
        }

        try {
            boolean acceptPriority = body.getOrDefault("acceptPriority", false);
            boolean acceptSummary = body.getOrDefault("acceptSummary", false);
            Issue updated = issueService.acceptAiTriage(id, acceptPriority, acceptSummary);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/issues/{id}/ignore-duplicate — Admin ignores AI duplicate suggestion.
     */
    @PatchMapping("/{id}/ignore-duplicate")
    public ResponseEntity<?> ignoreDuplicate(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can ignore duplicate suggestions"));
        }

        try {
            Issue updated = issueService.ignoreDuplicate(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/issues/{id}/ignore-triage — Admin dismisses all AI suggestions.
     */
    @PatchMapping("/{id}/ignore-triage")
    public ResponseEntity<?> ignoreAiTriage(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can ignore AI triage suggestions"));
        }

        try {
            Issue updated = issueService.ignoreAiTriage(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/issues/{id}/matching-developers — Get developers matching AI-suggested expertise.
     * ADMIN only.
     */
    @GetMapping("/{id}/matching-developers")
    public ResponseEntity<?> getMatchingDevelopers(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can view matching developers"));
        }

        Issue issue = issueService.getIssueById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        Expertise expertise = issue.getAiSuggestedExpertise();
        List<User> matchingDevs;

        if (expertise != null) {
            matchingDevs = userService.getDevelopersByExpertise(expertise);
            if (matchingDevs.isEmpty()) {
                // Fallback: show all developers
                matchingDevs = userService.getAllDevelopers();
            }
        } else {
            matchingDevs = userService.getAllDevelopers();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("suggestedExpertise", expertise);
        response.put("matchingDevelopers", matchingDevs);

        return ResponseEntity.ok(response);
    }

    // ── Mark as Duplicate (ADMIN only) ──────────────────────────────

    /**
     * PATCH /api/issues/{id}/mark-duplicate — Mark issue as duplicate of another.
     * Sets status to DUPLICATE, clears assignee, stores reference to original issue.
     * ADMIN only.
     */
    @PatchMapping("/{id}/mark-duplicate")
    public ResponseEntity<?> markAsDuplicate(@PathVariable Long id,
                                              @RequestBody Map<String, Long> body) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can mark issues as duplicates"));
        }

        try {
            Long originalIssueId = body.get("originalIssueId");
            if (originalIssueId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "originalIssueId is required"));
            }
            Issue updated = issueService.markAsDuplicate(id, originalIssueId);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Delete (ADMIN only) ─────────────────────────────────────────

    /**
     * DELETE /api/issues/{id} — Delete an issue. ADMIN only.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIssue(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can delete issues"));
        }

        try {
            issueService.deleteIssue(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Private Helpers ─────────────────────────────────────────────

    /**
     * Strips ALL AI analysis fields from issues for TESTER visibility.
     * Testers should NOT see any AI data (summary, priority, expertise, duplicate, reasoning).
     * But they CAN see duplicateOfIssueId to know if their issue was marked as duplicate.
     */
    private void stripAllAiFields(Issue issue) {
        issue.setAiSummary(null);
        issue.setAiSuggestedPriority(null);
        issue.setAiSuggestedExpertise(null);
        issue.setAiDuplicateBugId(null);
        issue.setAiDuplicateSimilarity(null);
        issue.setAiReason(null);
        issue.setTriageStatus(null);
    }

    /**
     * Strips duplicate analysis fields from issues for DEVELOPER visibility.
     * Developers see basic AI info but not duplicate detection details.
     */
    private void stripDuplicateFieldsForDeveloper(Issue issue) {
        issue.setAiDuplicateBugId(null);
        issue.setAiDuplicateSimilarity(null);
        // Also strip full AI panel fields — developer only needs issue details
        issue.setAiSummary(null);
        issue.setAiSuggestedPriority(null);
        issue.setAiSuggestedExpertise(null);
        issue.setAiReason(null);
        issue.setTriageStatus(null);
    }
}
