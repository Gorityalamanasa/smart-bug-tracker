package com.bugtracker.controller;

import com.bugtracker.model.User;
import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Role;
import com.bugtracker.security.CustomUserDetails;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for user management.
 * User management operations restricted to ADMIN role.
 * GET operations available to all authenticated users.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Get current authenticated user from SecurityContext */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser();
    }

    /** GET /api/users — List all users, optionally filtered by role or expertise */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Expertise expertise) {
        if (role != null && expertise != null) {
            return ResponseEntity.ok(userService.getDevelopersByExpertise(expertise));
        }
        if (role != null) {
            return ResponseEntity.ok(userService.getUsersByRole(role));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** GET /api/users/{id} — Get user by ID */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/users/me — Get current authenticated user's profile */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUserProfile() {
        return ResponseEntity.ok(getCurrentUser());
    }

    /** POST /api/users — Create a new user (ADMIN only) */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can create users"));
        }
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/users/{id} — Update a user (ADMIN only) */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @Valid @RequestBody User userDetails) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can update users"));
        }
        try {
            User updated = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** DELETE /api/users/{id} — Delete a user (ADMIN only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only ADMIN can delete users"));
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
