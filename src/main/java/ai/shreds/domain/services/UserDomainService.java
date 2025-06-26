package ai.shreds.domain.services;

import ai.shreds.domain.User;
import ai.shreds.domain.exceptions.DomainValidationException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for user-related business logic.
 */
@Service
public class UserDomainService {

    /**
     * Validates a user entity according to business rules.
     * @param user The user entity to validate
     * @throws DomainValidationException if validation fails
     */
    public void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        List<String> violations = new ArrayList<>();
        
        // Validate email format
        if (!isValidEmail(user.getEmail())) {
            violations.add("Invalid email format: " + user.getEmail());
        }
        
        // Validate name constraints
        if (user.getName() == null || user.getName().isBlank()) {
            violations.add("User name cannot be empty");
        }
        
        // Validate username constraints (e.g., length, allowed characters)
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            violations.add("Username cannot be empty");
        } else if (user.getUsername().length() < 3) {
            violations.add("Username must be at least 3 characters long");
        } else if (user.getUsername().length() > 50) {
            violations.add("Username cannot exceed 50 characters");
        } else if (!user.getUsername().matches("^[a-zA-Z0-9._-]+$")) {
            violations.add("Username can only contain letters, numbers, and the characters ._-");
        }
        
        if (!violations.isEmpty()) {
            throw new DomainValidationException(violations);
        }
    }
    
    /**
     * Checks if two usernames are equivalent (case-insensitive comparison).
     * @param username1 First username
     * @param username2 Second username
     * @return true if usernames are equivalent, false otherwise
     */
    public boolean areUsernamesEquivalent(String username1, String username2) {
        if (username1 == null || username2 == null) {
            return false;
        }
        return username1.equalsIgnoreCase(username2);
    }
    
    /**
     * Validates email format.
     * @param email The email to validate
     * @return true if email format is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        // Simple email validation pattern
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}