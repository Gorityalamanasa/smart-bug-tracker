package com.bugtracker.service;

import com.bugtracker.dto.AiTriageResponse;
import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import com.bugtracker.model.enums.TriageStatus;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service layer for issue management with business logic
 * for status transitions, assignment, AI triage, and filtering.
 */
@Service
public class IssueService {

    private static final Logger logger = LoggerFactory.getLogger(IssueService.class);

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final AiTriageService aiTriageService;

    /** Valid status transitions to enforce workflow */
    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = Map.of(
        Status.NEW,         Set.of(Status.OPEN, Status.CLOSED, Status.DUPLICATE),
        Status.OPEN,        Set.of(Status.IN_PROGRESS, Status.CLOSED),
        Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.OPEN),
        Status.RESOLVED,    Set.of(Status.CLOSED, Status.REOPENED),
        Status.CLOSED,      Set.of(Status.REOPENED),
        Status.REOPENED,    Set.of(Status.OPEN, Status.IN_PROGRESS),
        Status.DUPLICATE,   Set.of(Status.REOPENED)
    );

    public IssueService(IssueRepository issueRepository,
                         UserRepository userRepository,
                         AiTriageService aiTriageService) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.aiTriageService = aiTriageService;
    }

    public List<Issue> getAllIssues() {
        return issueRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Issue> getIssueById(Long id) {
        return issueRepository.findById(id);
    }

    public List<Issue> getIssuesByStatus(Status status) {
        return issueRepository.findByStatus(status);
    }

    public List<Issue> getIssuesByPriority(Priority priority) {
        return issueRepository.findByPriority(priority);
    }

    public List<Issue> getIssuesByAssignee(Long assigneeId) {
        return issueRepository.findByAssigneeId(assigneeId);
    }

    public List<Issue> getIssuesByReporter(Long reporterId) {
        return issueRepository.findByReporterId(reporterId);
    }

    /**
     * Creates a new issue with AI-assisted triage analysis.
     * 1. Save the issue immediately so the tester gets an ID right away.
     * 2. Fetch previous issues from the database for duplicate detection.
     * 3. Send title/description context to AI and store suggestions for admin review.
     */
    public Issue createIssueWithTriage(Issue issue, User reporter) {
        issue.setReporter(reporter);
        issue.setStatus(Status.NEW);

        Issue saved = issueRepository.save(issue);

        List<Issue> previousIssues = issueRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .filter(i -> !i.getId().equals(saved.getId()))
                .toList();

        try {
            AiTriageResponse triageResponse = aiTriageService.analyzeIssue(saved, previousIssues);

            saved.setAiSummary(triageResponse.getOptimizedSummary());
            saved.setAiSuggestedPriority(triageResponse.getSuggestedPriority());
            saved.setAiSuggestedExpertise(triageResponse.getSuggestedExpertise());
            saved.setAiDuplicateBugId(triageResponse.getDuplicateBugId());
            saved.setAiDuplicateSimilarity(triageResponse.getDuplicateSimilarity());
            saved.setAiReason(triageResponse.getReason());
            saved.setTriageStatus(TriageStatus.READY_FOR_TRIAGE);

            logger.info("AI triage completed for issue #{}: {}", saved.getId(), saved.getTitle());

        } catch (Exception e) {
            logger.error("AI triage failed for issue #{}: {}", saved.getId(), e.getMessage());
            saved.setTriageStatus(TriageStatus.READY_FOR_TRIAGE);
        }

        return issueRepository.save(saved);
    }

    /**
     * Creates a basic issue without AI triage (for backward compatibility).
     */
    public Issue createIssue(Issue issue) {
        issue.setStatus(Status.NEW);
        return issueRepository.save(issue);
    }

    public Issue updateIssue(Long id, Issue issueDetails) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        issue.setTitle(issueDetails.getTitle());
        issue.setDescription(issueDetails.getDescription());
        issue.setPriority(issueDetails.getPriority());

        if (issueDetails.getReporter() != null) {
            issue.setReporter(issueDetails.getReporter());
        }

        return issueRepository.save(issue);
    }

    /**
     * Changes issue status with validation of allowed transitions.
     */
    public Issue changeStatus(Long id, Status newStatus) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        Status currentStatus = issue.getStatus();
        Set<Status> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());

        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s. Allowed transitions: %s",
                    currentStatus, newStatus, allowed));
        }

        issue.setStatus(newStatus);
        return issueRepository.save(issue);
    }

    /**
     * Assigns an issue to a developer/user.
     */
    public Issue assignIssue(Long issueId, Long assigneeId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        if (issue.getStatus() == Status.DUPLICATE) {
            throw new IllegalStateException("Cannot assign a duplicate issue — work continues on the original issue");
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));

        issue.setAssignee(assignee);

        // Auto-transition from NEW to OPEN when assigned
        if (issue.getStatus() == Status.NEW) {
            issue.setStatus(Status.OPEN);
        }

        return issueRepository.save(issue);
    }

    /**
     * Admin accepts AI triage suggestions — copies AI fields to actual fields.
     */
    public Issue acceptAiTriage(Long issueId, boolean acceptPriority, boolean acceptSummary) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        if (acceptPriority && issue.getAiSuggestedPriority() != null) {
            issue.setPriority(issue.getAiSuggestedPriority());
        }

        if (acceptSummary && issue.getAiSummary() != null) {
            issue.setTitle(issue.getAiSummary());
        }

        issue.setTriageStatus(TriageStatus.TRIAGED);
        return issueRepository.save(issue);
    }

    /**
     * Admin ignores AI duplicate suggestion. Clears the duplicate suggestion fields.
     */
    public Issue ignoreDuplicate(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        issue.setAiDuplicateBugId(null);
        issue.setAiDuplicateSimilarity(null);
        issue.setTriageStatus(TriageStatus.TRIAGED);

        return issueRepository.save(issue);
    }

    /**
     * Admin dismisses all AI suggestions. Normal workflow continues with current issue values.
     */
    public Issue ignoreAiTriage(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        // Preserve AI data for audit trail — only mark as reviewed
        issue.setTriageStatus(TriageStatus.TRIAGED);

        return issueRepository.save(issue);
    }

    /**
     * Marks an issue as a duplicate of another issue.
     * Sets status to DUPLICATE, clears assignee, stores reference to original.
     */
    public Issue markAsDuplicate(Long issueId, Long originalIssueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        // Verify the original issue exists
        if (!issueRepository.existsById(originalIssueId)) {
            throw new RuntimeException("Original issue not found with id: " + originalIssueId);
        }

        issue.setDuplicateOfIssueId(originalIssueId);
        issue.setStatus(Status.DUPLICATE);
        issue.setAssignee(null);
        issue.setAiDuplicateBugId(null);
        issue.setAiDuplicateSimilarity(null);

        return issueRepository.save(issue);
    }

    public void deleteIssue(Long id) {
        if (!issueRepository.existsById(id)) {
            throw new RuntimeException("Issue not found with id: " + id);
        }
        issueRepository.deleteById(id);
    }

    /**
     * Returns dashboard statistics.
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIssues", issueRepository.count());
        stats.put("newIssues", issueRepository.countByStatus(Status.NEW));
        stats.put("openIssues", issueRepository.countByStatus(Status.OPEN));
        stats.put("inProgressIssues", issueRepository.countByStatus(Status.IN_PROGRESS));
        stats.put("resolvedIssues", issueRepository.countByStatus(Status.RESOLVED));
        stats.put("closedIssues", issueRepository.countByStatus(Status.CLOSED));
        stats.put("reopenedIssues", issueRepository.countByStatus(Status.REOPENED));
        stats.put("duplicateIssues", issueRepository.countByStatus(Status.DUPLICATE));
        stats.put("criticalIssues", issueRepository.countByPriority(Priority.CRITICAL));
        stats.put("highIssues", issueRepository.countByPriority(Priority.HIGH));
        stats.put("mediumIssues", issueRepository.countByPriority(Priority.MEDIUM));
        stats.put("lowIssues", issueRepository.countByPriority(Priority.LOW));
        return stats;
    }
}
