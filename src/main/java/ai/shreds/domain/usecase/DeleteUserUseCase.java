package ai.shreds.domain.usecase;

import ai.shreds.domain.User;
import ai.shreds.domain.ports.DomainOutputPortUserRepository;
import org.springframework.stereotype.Component;

/**
 * Use case for deleting a user by ID.
 * This use case encapsulates the business logic for user deletion.
 */
@Component
public class DeleteUserUseCase {
    
    private final DomainOutputPortUserRepository userRepository;
    
    public DeleteUserUseCase(DomainOutputPortUserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Executes the delete user use case.
     * @param userId The user ID to delete
     * @throws IllegalArgumentException if user with ID doesn't exist
     */
    public void execute(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Verify user exists before attempting deletion
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // Additional business rules for deletion could be added here
        // For example: check if user has active orders, etc.
        
        userRepository.deleteById(userId);
    }
    
    /**
     * Checks if a user can be deleted (business rules validation).
     * @param userId The user ID to check
     * @return true if user can be deleted, false otherwise
     */
    public boolean canDelete(Long userId) {
        if (userId == null) {
            return false;
        }
        
        // Check if user exists
        if (!userRepository.existsById(userId)) {
            return false;
        }
        
        // Add additional business rules here
        // For example: user cannot be deleted if they have active orders
        
        return true;
    }
}