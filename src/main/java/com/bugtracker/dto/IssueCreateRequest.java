package com.bugtracker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for creating a new issue.
 * Simplified: tester only needs to provide a title and description.
 */
public class IssueCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    public IssueCreateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
