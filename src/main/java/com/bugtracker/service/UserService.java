package com.bugtracker.service;

import com.bugtracker.model.User;
import com.bugtracker.model.enums.Expertise;
import com.bugtracker.model.enums.Role;
import com.bugtracker.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for user management operations.
 * Includes expertise-based developer lookup for AI triage.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Find developers matching a specific expertise.
     * FULL_STACK developers are included as they match all areas.
     */
    public List<User> getDevelopersByExpertise(Expertise expertise) {
        List<User> exactMatch = userRepository.findByRoleAndExpertise(Role.DEVELOPER, expertise);
        if (expertise != Expertise.FULL_STACK) {
            // Also include FULL_STACK developers as they can handle any area
            List<User> fullStackDevs = userRepository.findByRoleAndExpertise(
                    Role.DEVELOPER, Expertise.FULL_STACK);
            exactMatch.addAll(fullStackDevs);
        }
        return exactMatch;
    }

    /**
     * Get all developers (fallback when no expertise match exists).
     */
    public List<User> getAllDevelopers() {
        return userRepository.findByRole(Role.DEVELOPER);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        user.setUsername(userDetails.getUsername());
        user.setEmail(userDetails.getEmail());
        user.setRole(userDetails.getRole());
        if (userDetails.getExpertise() != null) {
            user.setExpertise(userDetails.getExpertise());
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
