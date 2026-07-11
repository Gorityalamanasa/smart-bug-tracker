package com.bugtracker.controller;

import com.bugtracker.dto.IssueCreateRequest;
import com.bugtracker.model.enums.Priority;
import com.bugtracker.model.enums.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import org.springframework.security.test.context.support.WithUserDetails;

/**
 * Integration tests for Issue REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithUserDetails("admin")
class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllIssues() throws Exception {
        mockMvc.perform(get("/api/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void shouldGetIssueById() throws Exception {
        mockMvc.perform(get("/api/issues/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    @Test
    void shouldReturn404ForNonExistentIssue() throws Exception {
        mockMvc.perform(get("/api/issues/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateIssue() throws Exception {
        IssueCreateRequest request = new IssueCreateRequest();
        request.setTitle("Test Issue from JUnit");
        request.setDescription("This is a test issue created by automated tests");

        mockMvc.perform(post("/api/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Issue from JUnit"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void shouldFilterIssuesByStatus() throws Exception {
        mockMvc.perform(get("/api/issues").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("NEW"))));
    }

    @Test
    void shouldGetDashboardStats() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIssues").isNumber())
                .andExpect(jsonPath("$.openIssues").isNumber());
    }

    @Test
    void shouldGetHealthCheck() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
