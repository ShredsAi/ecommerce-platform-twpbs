package ai.shreds.domain.usecase;

import ai.shreds.domain.User;
import ai.shreds.domain.ports.DomainOutputPortUserRepository;
import ai.shreds.domain.services.UserDomainService;
import ai.shreds.domain.exceptions.DomainValidationException;
import org.springframework.stereotype.Component;
import java.time.Instant;

/**
 * Use case for saving a user.
 * This use case encapsulates the business logic for creating or updating users.
 */
@Component
public class SaveUserUseCase {
    
    private final DomainOutputPortUserRepository userRepository;
    private final UserDomainService userDomainService;
    
    public SaveUserUseCase(DomainOutputPortUserRepository userRepository, 
                          UserDomainService userDomainService) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
    }
    
    /**
     * Executes the save user use case.
     * @param id The user ID (null for new user creation)
     * @param name The user's name
     * @param email The user's email
     * @param username The user's username
     * @return The saved user entity
     * @throws DomainValidationException if user data is invalid
     * @throws IllegalArgumentException if user with ID doesn't exist for update
     */
    public User execute(Long id, String name, String email, String username) {
        User user;
        
        if (id != null) {
            // Update existing user
            user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
            // Update user fields
            user = new User(id, name, email, username, user.getCreatedAt());
        } else {
            // Create new user
            user = new User(name, email, username);
        }
        
        // Validate the user using domain service
        userDomainService.validateUser(user);
        
        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            if (id == null || !userRepository.findById(id).map(u -> u.getEmail().equals(email)).orElse(false)) {
                throw new DomainValidationException("Email already exists: " + email);
            }
        }
        
        // Check for duplicate username
        if (userRepository.existsByUsername(username)) {
            if (id == null || !userRepository.findById(id).map(u -> u.getUsername().equals(username)).orElse(false)) {
                throw new DomainValidationException("Username already exists: " + username);
            }
        }
        
        return userRepository.save(user);
    }
}