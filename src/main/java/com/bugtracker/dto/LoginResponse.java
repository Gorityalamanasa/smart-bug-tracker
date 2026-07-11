package com.bugtracker.dto;

import com.bugtracker.model.enums.Role;

/**
 * Response payload after successful login, includes JWT token.
 */
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private Role role;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String username, Role role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
