package com.bugtracker.config;

import com.bugtracker.model.Comment;
import com.bugtracker.model.Issue;
import com.bugtracker.model.User;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Role;
import com.bugtracker.model.enums.Status;
import com.bugtracker.repository.CommentRepository;
import com.bugtracker.repository.IssueRepository;
import com.bugtracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with sample users, issues, and comments on startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;
    private final CommentRepository commentRepository;

    public DataInitializer(UserRepository userRepository,
                           IssueRepository issueRepository,
                           CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public void run(String... args) {
        // Only seed if database is empty
        if (userRepository.count() > 0) return;

        // Create sample users
        User admin = userRepository.save(new User("admin", "admin@bugtracker.com", Role.ADMIN));
        User dev1 = userRepository.save(new User("john_dev", "john@bugtracker.com", Role.DEVELOPER));
        User dev2 = userRepository.save(new User("jane_dev", "jane@bugtracker.com", Role.DEVELOPER));
        User tester1 = userRepository.save(new User("alice_qa", "alice@bugtracker.com", Role.TESTER));
        User tester2 = userRepository.save(new User("bob_qa", "bob@bugtracker.com", Role.TESTER));

        // Create sample issues
        Issue issue1 = new Issue("Login page crashes on invalid input",
                "When entering special characters in the login form, the application crashes with a NullPointerException. Steps to reproduce: 1. Go to login page 2. Enter '<script>' in username 3. Click submit",
                Priority.CRITICAL, tester1);
        issue1.setAssignee(dev1);
        issue1.setStatus(Status.IN_PROGRESS);
        issue1 = issueRepository.save(issue1);

        Issue issue2 = new Issue("Dashboard loading time exceeds 10 seconds",
                "The main dashboard takes over 10 seconds to load when there are more than 100 issues. Performance profiling shows N+1 query problem in issue listing.",
                Priority.HIGH, tester2);
        issue2.setAssignee(dev2);
        issue2.setStatus(Status.OPEN);
        issue2 = issueRepository.save(issue2);

        Issue issue3 = new Issue("Add export to CSV feature",
                "Users have requested the ability to export issue lists to CSV format for reporting. Should include all fields and support filtered exports.",
                Priority.MEDIUM, admin);
        issue3.setStatus(Status.NEW);
        issue3 = issueRepository.save(issue3);

        Issue issue4 = new Issue("Typo in error message on settings page",
                "The error message on the settings page reads 'Setings saved successfully' — missing a 't' in Settings.",
                Priority.LOW, tester1);
        issue4.setAssignee(dev1);
        issue4.setStatus(Status.RESOLVED);
        issue4 = issueRepository.save(issue4);

        Issue issue5 = new Issue("API rate limiting not enforced",
                "The REST API does not enforce rate limiting, allowing unlimited requests per minute. This is a security concern and could lead to DDoS attacks.",
                Priority.HIGH, tester2);
        issue5.setStatus(Status.NEW);
        issue5 = issueRepository.save(issue5);

        Issue issue6 = new Issue("Mobile responsive layout broken",
                "On screens smaller than 768px, the navigation menu overlaps with the main content area. Tested on Chrome mobile and Safari iOS.",
                Priority.MEDIUM, tester1);
        issue6.setAssignee(dev2);
        issue6.setStatus(Status.IN_PROGRESS);
        issue6 = issueRepository.save(issue6);

        Issue issue7 = new Issue("Database connection pool exhaustion",
                "Under load testing with 50 concurrent users, the application runs out of database connections after about 5 minutes. Need to review HikariCP pool configuration.",
                Priority.CRITICAL, dev1);
        issue7.setAssignee(dev1);
        issue7.setStatus(Status.OPEN);
        issue7 = issueRepository.save(issue7);

        Issue issue8 = new Issue("Update documentation for API v2",
                "The API documentation still references v1 endpoints. Need to update all endpoint descriptions, request/response examples for the v2 API.",
                Priority.LOW, admin);
        issue8.setStatus(Status.CLOSED);
        issue8 = issueRepository.save(issue8);

        // Add sample comments
        commentRepository.save(new Comment(
                "I can reproduce this consistently. The stack trace points to InputValidator.java line 42.",
                dev1, issue1));
        commentRepository.save(new Comment(
                "Working on a fix now. Need to add proper input sanitization before processing.",
                dev1, issue1));
        commentRepository.save(new Comment(
                "This might be related to the Hibernate lazy loading issue we had last sprint.",
                dev2, issue2));
        commentRepository.save(new Comment(
                "Fixed by adding @BatchSize annotation and optimizing the query. PR submitted.",
                dev2, issue2));
        commentRepository.save(new Comment(
                "Low priority but easy fix — I'll include it in my next commit.",
                dev1, issue4));

        System.out.println("✓ Database seeded with " + userRepository.count() + " users and "
                + issueRepository.count() + " issues");
    }
}
