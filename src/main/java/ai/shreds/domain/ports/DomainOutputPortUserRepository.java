package ai.shreds.domain.ports;

import ai.shreds.domain.User;
import java.util.Optional;
import java.util.List;

/**
 * Domain output port for user repository operations.
 * This port defines the contract for user persistence operations.
 */
public interface DomainOutputPortUserRepository {

    /**
     * Saves a user entity.
     * @param user The user entity to save
     * @return The saved user entity
     * @throws ai.shreds.domain.exceptions.DomainValidationException if user data is invalid
     */
    User save(User user);

    /**
     * Finds a user by its ID.
     * @param id The user ID to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findById(Long id);

    /**
     * Finds all users.
     * @return List of all users
     */
    List<User> findAll();

    /**
     * Deletes a user by its ID.
     * @param id The user ID to delete
     */
    void deleteById(Long id);

    /**
     * Finds a user by email address.
     * @param email The email to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by username.
     * @param username The username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user exists with the given ID.
     * @param id The user ID to check
     * @return true if user exists, false otherwise
     */
    boolean existsById(Long id);

    /**
     * Checks if a user exists with the given email.
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     * @param username The username to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);
}