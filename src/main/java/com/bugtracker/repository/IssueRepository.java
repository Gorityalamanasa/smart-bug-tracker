package com.bugtracker.repository;

import com.bugtracker.model.Issue;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for Issue CRUD operations and custom queries.
 */
@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByStatus(Status status);

    List<Issue> findByPriority(Priority priority);

    List<Issue> findByAssigneeId(Long assigneeId);

    List<Issue> findByReporterId(Long reporterId);

    List<Issue> findByStatusAndPriority(Status status, Priority priority);

    long countByStatus(Status status);

    long countByPriority(Priority priority);

    List<Issue> findAllByOrderByCreatedAtDesc();

    /** Fetch recent issues for AI duplicate comparison */
    List<Issue> findTop50ByOrderByCreatedAtDesc();
}
