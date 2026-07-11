package com.bugtracker.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import com.bugtracker.model.enums.TriageStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a bug/issue in the tracking system.
 * Includes AI-assisted triage fields for intelligent bug analysis.
 */
@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Comment> comments = new ArrayList<>();

    // ── AI Triage Fields ──────────────────────────────────────────

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_suggested_priority")
    private Priority aiSuggestedPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_suggested_expertise")
    private Expertise aiSuggestedExpertise;

    @Column(name = "ai_duplicate_bug_id")
    private Long aiDuplicateBugId;

    @Column(name = "ai_duplicate_similarity")
    private String aiDuplicateSimilarity;

    @Column(name = "ai_reason", columnDefinition = "TEXT")
    private String aiReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "triage_status")
    private TriageStatus triageStatus;

    // ── Duplicate Tracking ────────────────────────────────────────

    @Column(name = "duplicate_of_issue_id")
    private Long duplicateOfIssueId;

    // ── Timestamps ────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Issue() {}

    public Issue(String title, String description, Priority priority, User reporter) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.reporter = reporter;
        this.status = Status.NEW;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters and Setters ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public Priority getAiSuggestedPriority() { return aiSuggestedPriority; }
    public void setAiSuggestedPriority(Priority aiSuggestedPriority) { this.aiSuggestedPriority = aiSuggestedPriority; }

    public Expertise getAiSuggestedExpertise() { return aiSuggestedExpertise; }
    public void setAiSuggestedExpertise(Expertise aiSuggestedExpertise) { this.aiSuggestedExpertise = aiSuggestedExpertise; }

    public Long getAiDuplicateBugId() { return aiDuplicateBugId; }
    public void setAiDuplicateBugId(Long aiDuplicateBugId) { this.aiDuplicateBugId = aiDuplicateBugId; }

    public String getAiDuplicateSimilarity() { return aiDuplicateSimilarity; }
    public void setAiDuplicateSimilarity(String aiDuplicateSimilarity) { this.aiDuplicateSimilarity = aiDuplicateSimilarity; }

    public String getAiReason() { return aiReason; }
    public void setAiReason(String aiReason) { this.aiReason = aiReason; }

    public TriageStatus getTriageStatus() { return triageStatus; }
    public void setTriageStatus(TriageStatus triageStatus) { this.triageStatus = triageStatus; }

    public Long getDuplicateOfIssueId() { return duplicateOfIssueId; }
    public void setDuplicateOfIssueId(Long duplicateOfIssueId) { this.duplicateOfIssueId = duplicateOfIssueId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
