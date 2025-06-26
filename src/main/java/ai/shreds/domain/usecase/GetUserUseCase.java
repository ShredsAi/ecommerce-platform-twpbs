package ai.shreds.domain.usecase;

import ai.shreds.domain.User;
import ai.shreds.domain.ports.DomainOutputPortUserRepository;
import ai.shreds.domain.exceptions.DomainValidationException;
import org.springframework.stereotype.Component;

/**
 * Use case for retrieving a user by ID.
 * This use case encapsulates the business logic for finding users.
 */
@Component
public class GetUserUseCase {
    
    private final DomainOutputPortUserRepository userRepository;
    
    public GetUserUseCase(DomainOutputPortUserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Executes the get user use case.
     * @param userId The user ID to retrieve
     * @return The user entity
     * @throws IllegalArgumentException if user with ID doesn't exist
     */
    public User execute(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }
    
    /**
     * Executes the get user by email use case.
     * @param email The email to search for
     * @return The user entity
     * @throws IllegalArgumentException if user with email doesn't exist
     */
    public User executeByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
    
    /**
     * Executes the get user by username use case.
     * @param username The username to search for
     * @return The user entity
     * @throws IllegalArgumentException if user with username doesn't exist
     */
    public User executeByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }
}