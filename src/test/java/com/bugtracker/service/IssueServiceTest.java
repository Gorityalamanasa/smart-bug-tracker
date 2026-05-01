package com.bugtracker.service;

import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Role;
import com.bugtracker.model.enums.Status;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IssueService business logic.
 */
@SpringBootTest
class IssueServiceTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateIssueWithNewStatus() {
        Issue issue = new Issue();
        issue.setTitle("Service Test Issue");
        issue.setDescription("Testing issue creation");
        issue.setPriority(Priority.MEDIUM);

        Issue created = issueService.createIssue(issue);

        assertNotNull(created.getId());
        assertEquals(Status.NEW, created.getStatus());
        assertEquals("Service Test Issue", created.getTitle());
    }

    @Test
    void shouldChangeStatusWithValidTransition() {
        Issue issue = new Issue();
        issue.setTitle("Status Transition Test");
        issue.setPriority(Priority.LOW);
        Issue created = issueService.createIssue(issue);

        // NEW -> OPEN is valid
        Issue updated = issueService.changeStatus(created.getId(), Status.OPEN);
        assertEquals(Status.OPEN, updated.getStatus());

        // OPEN -> IN_PROGRESS is valid
        updated = issueService.changeStatus(created.getId(), Status.IN_PROGRESS);
        assertEquals(Status.IN_PROGRESS, updated.getStatus());
    }

    @Test
    void shouldRejectInvalidStatusTransition() {
        Issue issue = new Issue();
        issue.setTitle("Invalid Transition Test");
        issue.setPriority(Priority.LOW);
        Issue created = issueService.createIssue(issue);

        // NEW -> RESOLVED is not valid
        assertThrows(IllegalStateException.class, () -> {
            issueService.changeStatus(created.getId(), Status.RESOLVED);
        });
    }

    @Test
    void shouldAutoTransitionToOpenOnAssignment() {
        // Create a user to assign
        User dev = userRepository.save(new User("test_assign_dev", "testassign@test.com", Role.DEVELOPER));

        Issue issue = new Issue();
        issue.setTitle("Auto Transition Test");
        issue.setPriority(Priority.MEDIUM);
        Issue created = issueService.createIssue(issue);

        assertEquals(Status.NEW, created.getStatus());

        // Assigning should auto-transition from NEW to OPEN
        Issue assigned = issueService.assignIssue(created.getId(), dev.getId());
        assertEquals(Status.OPEN, assigned.getStatus());
        assertEquals(dev.getId(), assigned.getAssignee().getId());
    }

    @Test
    void shouldReturnDashboardStats() {
        var stats = issueService.getDashboardStats();
        assertNotNull(stats);
        assertTrue((Long) stats.get("totalIssues") > 0);
    }
}
