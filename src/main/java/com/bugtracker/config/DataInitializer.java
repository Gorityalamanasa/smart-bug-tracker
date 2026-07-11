package com.bugtracker.config;

import com.bugtracker.model.Comment;
import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Role;
import com.bugtracker.model.enums.Status;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import com.bugtracker.service.IssueService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with sample users (with BCrypt passwords),
 * issues, and comments on startup.
 *
 * Default password for all users: password123
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IssueService issueService;

    public DataInitializer(UserRepository userRepository,
                           IssueRepository issueRepository,
                           CommentRepository commentRepository,
                           PasswordEncoder passwordEncoder,
                           IssueService issueService) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
        this.issueService = issueService;
    }

    @Override
    public void run(String... args) {
        // Only seed if database is empty
        if (userRepository.count() > 0) return;

        String defaultPassword = passwordEncoder.encode("password123");

        // ── ADMIN ─────────────────────────────────────────────────
        User admin = userRepository.save(
                new User("admin", "admin@bugtracker.com", defaultPassword, Role.ADMIN));

        // ── DEVELOPERS (with expertise) ───────────────────────────
        User rahul = userRepository.save(
                new User("rahul", "rahul@bugtracker.com", defaultPassword, Role.DEVELOPER, Expertise.BACKEND));

        User priya = userRepository.save(
                new User("priya", "priya@bugtracker.com", defaultPassword, Role.DEVELOPER, Expertise.FRONTEND));

        User arjun = userRepository.save(
                new User("arjun", "arjun@bugtracker.com", defaultPassword, Role.DEVELOPER, Expertise.DATABASE));

        User neha = userRepository.save(
                new User("neha", "neha@bugtracker.com", defaultPassword, Role.DEVELOPER, Expertise.FULL_STACK));

        // ── TESTERS ───────────────────────────────────────────────
        User tester1 = userRepository.save(
                new User("tester1", "tester1@bugtracker.com", defaultPassword, Role.TESTER));

        User tester2 = userRepository.save(
                new User("tester2", "tester2@bugtracker.com", defaultPassword, Role.TESTER));

        User tester3 = userRepository.save(
                new User("tester3", "tester3@bugtracker.com", defaultPassword, Role.TESTER));

        // ── SAMPLE ISSUES ─────────────────────────────────────────

        Issue issue1 = new Issue("Login page crashes on invalid input",
                "When entering special characters in the login form, the application crashes with a NullPointerException. Steps to reproduce: 1. Go to login page 2. Enter '<script>' in username 3. Click submit",
                Priority.CRITICAL, tester1);
        issue1 = issueService.createIssueWithTriage(issue1, tester1);
        issue1.setAssignee(rahul);
        issue1.setStatus(Status.IN_PROGRESS);
        issue1.setPriority(Priority.CRITICAL);
        issue1 = issueRepository.save(issue1);

        Issue issue2 = new Issue("Dashboard loading time exceeds 10 seconds",
                "The main dashboard takes over 10 seconds to load when there are more than 100 issues. Performance profiling shows N+1 query problem in issue listing.",
                Priority.HIGH, tester2);
        issue2 = issueService.createIssueWithTriage(issue2, tester2);
        issue2.setAssignee(arjun);
        issue2.setStatus(Status.OPEN);
        issue2.setPriority(Priority.HIGH);
        issue2 = issueRepository.save(issue2);

        Issue issue3 = new Issue("Add export to CSV feature",
                "Users have requested the ability to export issue lists to CSV format for reporting. Should include all fields and support filtered exports.",
                Priority.MEDIUM, admin);
        issue3 = issueService.createIssueWithTriage(issue3, admin);
        issue3.setStatus(Status.NEW);
        issue3.setPriority(Priority.MEDIUM);
        issue3 = issueRepository.save(issue3);

        Issue issue4 = new Issue("Typo in error message on settings page",
                "The error message on the settings page reads 'Setings saved successfully' — missing a 't' in Settings.",
                Priority.LOW, tester1);
        issue4 = issueService.createIssueWithTriage(issue4, tester1);
        issue4.setAssignee(priya);
        issue4.setStatus(Status.RESOLVED);
        issue4.setPriority(Priority.LOW);
        issue4 = issueRepository.save(issue4);

        Issue issue5 = new Issue("API rate limiting not enforced",
                "The REST API does not enforce rate limiting, allowing unlimited requests per minute. This is a security concern and could lead to DDoS attacks.",
                Priority.HIGH, tester2);
        issue5 = issueService.createIssueWithTriage(issue5, tester2);
        issue5.setStatus(Status.NEW);
        issue5.setPriority(Priority.HIGH);
        issue5 = issueRepository.save(issue5);

        Issue issue6 = new Issue("Mobile responsive layout broken",
                "On screens smaller than 768px, the navigation menu overlaps with the main content area. Tested on Chrome mobile and Safari iOS.",
                Priority.MEDIUM, tester1);
        issue6 = issueService.createIssueWithTriage(issue6, tester1);
        issue6.setAssignee(priya);
        issue6.setStatus(Status.IN_PROGRESS);
        issue6.setPriority(Priority.MEDIUM);
        issue6 = issueRepository.save(issue6);

        Issue issue7 = new Issue("Database connection pool exhaustion",
                "Under load testing with 50 concurrent users, the application runs out of database connections after about 5 minutes. Need to review HikariCP pool configuration.",
                Priority.CRITICAL, tester3);
        issue7 = issueService.createIssueWithTriage(issue7, tester3);
        issue7.setAssignee(arjun);
        issue7.setStatus(Status.OPEN);
        issue7.setPriority(Priority.CRITICAL);
        issue7 = issueRepository.save(issue7);

        Issue issue8 = new Issue("Update documentation for API v2",
                "The API documentation still references v1 endpoints. Need to update all endpoint descriptions, request/response examples for the v2 API.",
                Priority.LOW, admin);
        issue8 = issueService.createIssueWithTriage(issue8, admin);
        issue8.setStatus(Status.CLOSED);
        issue8.setPriority(Priority.LOW);
        issue8 = issueRepository.save(issue8);

        // ── SAMPLE COMMENTS ───────────────────────────────────────
        commentRepository.save(new Comment(
                "I can reproduce this consistently. The stack trace points to InputValidator.java line 42.",
                rahul, issue1));
        commentRepository.save(new Comment(
                "Working on a fix now. Need to add proper input sanitization before processing.",
                rahul, issue1));
        commentRepository.save(new Comment(
                "This might be related to the Hibernate lazy loading issue we had last sprint.",
                arjun, issue2));
        commentRepository.save(new Comment(
                "Fixed by adding @BatchSize annotation and optimizing the query. PR submitted.",
                arjun, issue2));
        commentRepository.save(new Comment(
                "Low priority but easy fix — I'll include it in my next commit.",
                priya, issue4));

        System.out.println("✓ Database seeded with " + userRepository.count() + " users and "
                + issueRepository.count() + " issues");
        System.out.println("  Default password for all users: password123");
    }
}
