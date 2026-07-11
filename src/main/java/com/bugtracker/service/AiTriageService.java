package com.bugtracker.service;

import com.bugtracker.dto.AiTriageResponse;
import com.bugtracker.model.Issue;
import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-Assisted Bug Triage Service.
 *
 * Uses Google Gemini API for intelligent bug analysis. If the API is
 * unavailable or unconfigured, falls back to rule-based keyword analysis.
 *
 * AI NEVER automatically assigns developers, changes priority, or closes issues.
 * It only provides suggestions for the Admin to review.
 */
@Service
public class AiTriageService {

    private static final Logger logger = LoggerFactory.getLogger(AiTriageService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String geminiApiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AiTriageService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyzes a new issue using AI (Gemini) or rule-based fallback.
     *
     * @param newIssue        the new issue being created
     * @param previousIssues  recent issues from database (for duplicate detection)
     * @return AI triage suggestions
     */
    public AiTriageResponse analyzeIssue(Issue newIssue, List<Issue> previousIssues) {
        // Try AI-based analysis first
        if (StringUtils.hasText(geminiApiKey)) {
            try {
                return callGeminiApi(newIssue, previousIssues);
            } catch (Exception e) {
                logger.warn("Gemini API call failed, falling back to rule-based triage: {}", e.getMessage());
            }
        } else {
            logger.info("Gemini API key not configured, using rule-based triage");
        }

        // Fallback to rule-based analysis
        return ruleBasedFallback(newIssue, previousIssues);
    }

    /**
     * Calls Google Gemini API with a structured prompt for bug triage.
     */
    private AiTriageResponse callGeminiApi(Issue newIssue, List<Issue> previousIssues) {
        String prompt = buildPrompt(newIssue, previousIssues);

        // Build request body for Gemini API
        String requestBody = buildGeminiRequestBody(prompt);

        String apiUrlWithKey = geminiApiUrl + "?key=" + geminiApiKey;

        String responseBody = webClient.post()
                .uri(apiUrlWithKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        return parseGeminiResponse(responseBody);
    }

    /**
     * Builds a structured prompt for the AI model.
     * Sends only title and description of the new issue,
     * plus Bug ID, title, and description of previous issues for duplicate detection.
     */
    private String buildPrompt(Issue newIssue, List<Issue> previousIssues) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a bug triage assistant for a software development team. ");
        prompt.append("Analyze the following new bug report and provide triage suggestions.\n\n");

        prompt.append("=== NEW BUG REPORT ===\n");
        prompt.append("Title: ").append(newIssue.getTitle()).append("\n");
        prompt.append("Description: ").append(safeString(newIssue.getDescription())).append("\n\n");

        if (!previousIssues.isEmpty()) {
            prompt.append("=== PREVIOUS ISSUES (for duplicate detection) ===\n");
            for (Issue prev : previousIssues) {
                prompt.append("Bug #").append(prev.getId())
                      .append(" | Title: ").append(prev.getTitle())
                      .append(" | Description: ").append(truncate(safeString(prev.getDescription()), 200))
                      .append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("=== RESPOND IN THIS EXACT JSON FORMAT ===\n");
        prompt.append("{\n");
        prompt.append("  \"optimizedSummary\": \"A clear, concise summary of the bug\",\n");
        prompt.append("  \"suggestedPriority\": \"LOW|MEDIUM|HIGH|CRITICAL\",\n");
        prompt.append("  \"suggestedExpertise\": \"BACKEND|FRONTEND|DATABASE|FULL_STACK\",\n");
        prompt.append("  \"duplicateBugId\": null or bug ID number,\n");
        prompt.append("  \"duplicateSimilarity\": \"0%\" to \"100%\" or null if no duplicate,\n");
        prompt.append("  \"reason\": \"Explain why you suggested this priority, expertise, and duplicate\"\n");
        prompt.append("}\n\n");
        prompt.append("IMPORTANT: Respond with ONLY valid JSON, no markdown formatting, no code blocks.");

        return prompt.toString();
    }

    /**
     * Builds the Gemini API request body JSON.
     */
    private String buildGeminiRequestBody(String prompt) {
        try {
            Map<String, Object> textPart = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(textPart));
            Map<String, Object> requestMap = Map.of("contents", List.of(content));
            return objectMapper.writeValueAsString(requestMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Gemini request body", e);
        }
    }

    /**
     * Parses the Gemini API response to extract triage suggestions.
     */
    private AiTriageResponse parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                String text = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                // Clean markdown formatting if present
                text = text.replaceAll("```json\\s*", "")
                           .replaceAll("```\\s*", "")
                           .trim();

                JsonNode aiResult = objectMapper.readTree(text);

                AiTriageResponse response = new AiTriageResponse();
                response.setOptimizedSummary(getJsonString(aiResult, "optimizedSummary"));
                response.setSuggestedPriority(parseEnum(Priority.class, getJsonString(aiResult, "suggestedPriority")));
                response.setSuggestedExpertise(parseEnum(Expertise.class, getJsonString(aiResult, "suggestedExpertise")));

                if (!aiResult.path("duplicateBugId").isNull() && aiResult.has("duplicateBugId")) {
                    try {
                        response.setDuplicateBugId(aiResult.get("duplicateBugId").asLong());
                    } catch (Exception ignored) {}
                }

                response.setDuplicateSimilarity(getJsonString(aiResult, "duplicateSimilarity"));
                response.setReason(getJsonString(aiResult, "reason"));

                return response;
            }

            throw new RuntimeException("Empty response from Gemini API");

        } catch (Exception e) {
            logger.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    // ── Rule-Based Fallback ─────────────────────────────────────────

    /**
     * Rule-based triage fallback when AI API is unavailable.
     * Uses keyword matching for priority and expertise suggestions.
     */
    public AiTriageResponse ruleBasedFallback(Issue newIssue, List<Issue> previousIssues) {
        AiTriageResponse response = new AiTriageResponse();

        String combinedText = (safeString(newIssue.getTitle()) + " " +
                safeString(newIssue.getDescription())).toLowerCase();

        // Priority suggestion based on keywords
        response.setSuggestedPriority(suggestPriority(combinedText));

        // Expertise suggestion based on keywords
        response.setSuggestedExpertise(suggestExpertise(combinedText));

        // Generate optimized summary
        response.setOptimizedSummary(generateSummary(newIssue));

        // Check for duplicates using Jaccard similarity
        findDuplicate(newIssue, previousIssues, response);

        // Build reason
        response.setReason(buildRuleBasedReason(response));

        return response;
    }

    /** Suggest priority based on keyword patterns */
    private Priority suggestPriority(String text) {
        String[] criticalKeywords = {"crash", "data loss", "security", "vulnerability",
                "breach", "down", "outage", "production down"};
        String[] highKeywords = {"payment failed", "500", "error 500", "authentication",
                "cannot login", "broken", "not working", "exception"};
        String[] lowKeywords = {"typo", "spelling", "cosmetic", "minor", "color",
                "font", "alignment", "tooltip"};

        for (String kw : criticalKeywords) {
            if (text.contains(kw)) return Priority.CRITICAL;
        }
        for (String kw : highKeywords) {
            if (text.contains(kw)) return Priority.HIGH;
        }
        for (String kw : lowKeywords) {
            if (text.contains(kw)) return Priority.LOW;
        }
        return Priority.MEDIUM;
    }

    /** Suggest expertise based on keyword patterns */
    private Expertise suggestExpertise(String text) {
        String[] backendKeywords = {"api", "endpoint", "server", "rest", "microservice",
                "backend", "controller", "service", "response", "request", "500", "timeout"};
        String[] frontendKeywords = {"css", "button", "layout", "page", "ui", "ux",
                "frontend", "display", "modal", "form", "responsive", "html", "javascript"};
        String[] databaseKeywords = {"database", "query", "sql", "table", "migration",
                "index", "slow query", "connection pool", "hibernate", "jpa"};

        int backendScore = 0, frontendScore = 0, databaseScore = 0;

        for (String kw : backendKeywords) {
            if (text.contains(kw)) backendScore++;
        }
        for (String kw : frontendKeywords) {
            if (text.contains(kw)) frontendScore++;
        }
        for (String kw : databaseKeywords) {
            if (text.contains(kw)) databaseScore++;
        }

        if (backendScore == 0 && frontendScore == 0 && databaseScore == 0) {
            return Expertise.FULL_STACK;
        }

        int max = Math.max(backendScore, Math.max(frontendScore, databaseScore));
        if (max == backendScore && max == frontendScore) return Expertise.FULL_STACK;
        if (max == backendScore) return Expertise.BACKEND;
        if (max == frontendScore) return Expertise.FRONTEND;
        return Expertise.DATABASE;
    }

    /** Generate a simple summary from the title and description */
    private String generateSummary(Issue issue) {
        String title = safeString(issue.getTitle());
        String desc = safeString(issue.getDescription());
        if (desc.length() > 100) {
            return title + " — " + desc.substring(0, 100) + "...";
        }
        return StringUtils.hasText(desc) ? title + " — " + desc : title;
    }

    /**
     * Find potential duplicates using weighted similarity.
     * Uses both title-only and full-text Jaccard comparison.
     * Title similarity gets higher weight (70%) since titles are concise and comparable,
     * while description length differences can skew full-text similarity.
     */
    private void findDuplicate(Issue newIssue, List<Issue> previousIssues,
                               AiTriageResponse response) {
        String newTitle = safeString(newIssue.getTitle()).toLowerCase();
        String newFullText = (newTitle + " " + safeString(newIssue.getDescription())).toLowerCase();
        Set<String> newTitleWords = tokenize(newTitle);
        Set<String> newFullWords = tokenize(newFullText);

        Long bestMatchId = null;
        double bestSimilarity = 0;

        for (Issue prev : previousIssues) {
            // Skip comparing an issue with itself
            if (prev.getId() != null && prev.getId().equals(newIssue.getId())) continue;

            String prevTitle = safeString(prev.getTitle()).toLowerCase();
            String prevFullText = (prevTitle + " " + safeString(prev.getDescription())).toLowerCase();
            Set<String> prevTitleWords = tokenize(prevTitle);
            Set<String> prevFullWords = tokenize(prevFullText);

            // Weighted similarity: 70% title + 30% full text
            double titleSim = jaccardSimilarity(newTitleWords, prevTitleWords);
            double fullSim = jaccardSimilarity(newFullWords, prevFullWords);
            double weightedSim = (titleSim * 0.7) + (fullSim * 0.3);

            if (weightedSim > bestSimilarity && weightedSim >= 0.35) {
                bestSimilarity = weightedSim;
                bestMatchId = prev.getId();
            }
        }

        if (bestMatchId != null) {
            response.setDuplicateBugId(bestMatchId);
            response.setDuplicateSimilarity(Math.round(bestSimilarity * 100) + "%");
        }
    }

    /** Jaccard similarity coefficient between two word sets */
    private double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() && set2.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    /** Tokenize text into meaningful words (length > 2) */
    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(w -> w.length() > 2)
                .collect(Collectors.toSet());
    }

    /** Build explanation for rule-based suggestions */
    private String buildRuleBasedReason(AiTriageResponse response) {
        StringBuilder reason = new StringBuilder("[Rule-Based Analysis] ");
        reason.append("Priority suggested as ").append(response.getSuggestedPriority())
              .append(" based on keyword analysis. ");
        reason.append("Expertise area ").append(response.getSuggestedExpertise())
              .append(" identified from technical terms in the description.");

        if (response.getDuplicateBugId() != null) {
            reason.append(" Possible duplicate of Bug #").append(response.getDuplicateBugId())
                  .append(" with ").append(response.getDuplicateSimilarity()).append(" similarity.");
        }
        return reason.toString();
    }

    // ── Utility Methods ─────────────────────────────────────────────

    private String safeString(String s) {
        return s != null ? s : "";
    }

    private String truncate(String s, int maxLength) {
        return s.length() > maxLength ? s.substring(0, maxLength) + "..." : s;
    }

    private String getJsonString(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNull() || value.isMissingNode() ? null : value.asText();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
