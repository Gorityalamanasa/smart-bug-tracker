package com.bugtracker.dto;

import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Priority;

/**
 * DTO for AI triage analysis response.
 * Contains all AI-generated suggestions for a bug report.
 */
public class AiTriageResponse {

    private String optimizedSummary;
    private Priority suggestedPriority;
    private Expertise suggestedExpertise;
    private Long duplicateBugId;
    private String duplicateSimilarity;
    private String reason;

    public AiTriageResponse() {}

    public String getOptimizedSummary() { return optimizedSummary; }
    public void setOptimizedSummary(String optimizedSummary) { this.optimizedSummary = optimizedSummary; }

    public Priority getSuggestedPriority() { return suggestedPriority; }
    public void setSuggestedPriority(Priority suggestedPriority) { this.suggestedPriority = suggestedPriority; }

    public Expertise getSuggestedExpertise() { return suggestedExpertise; }
    public void setSuggestedExpertise(Expertise suggestedExpertise) { this.suggestedExpertise = suggestedExpertise; }

    public Long getDuplicateBugId() { return duplicateBugId; }
    public void setDuplicateBugId(Long duplicateBugId) { this.duplicateBugId = duplicateBugId; }

    public String getDuplicateSimilarity() { return duplicateSimilarity; }
    public void setDuplicateSimilarity(String duplicateSimilarity) { this.duplicateSimilarity = duplicateSimilarity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
