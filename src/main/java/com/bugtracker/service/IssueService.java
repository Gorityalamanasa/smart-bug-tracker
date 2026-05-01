package com.bugtracker.service;

import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Service layer for issue management with business logic
 * for status transitions, assignment, and filtering.
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    /** Valid status transitions to enforce workflow */
    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = Map.of(
        Status.NEW,         Set.of(Status.OPEN, Status.CLOSED),
        Status.OPEN,        Set.of(Status.IN_PROGRESS, Status.CLOSED),
        Status.IN_PROGRESS, Set.of(Status.RESOLVED, Status.OPEN),
        Status.RESOLVED,    Set.of(Status.CLOSED, Status.OPEN),
        Status.CLOSED,      Set.of(Status.OPEN)
    );

    public IssueService(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
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

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));

        issue.setAssignee(assignee);

        // Auto-transition from NEW to OPEN when assigned
        if (issue.getStatus() == Status.NEW) {
            issue.setStatus(Status.OPEN);
        }

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
        stats.put("criticalIssues", issueRepository.countByPriority(Priority.CRITICAL));
        stats.put("highIssues", issueRepository.countByPriority(Priority.HIGH));
        stats.put("mediumIssues", issueRepository.countByPriority(Priority.MEDIUM));
        stats.put("lowIssues", issueRepository.countByPriority(Priority.LOW));
        return stats;
    }
}
