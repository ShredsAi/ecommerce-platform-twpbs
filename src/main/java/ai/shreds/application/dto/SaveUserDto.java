package ai.shreds.application.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/**
 * Application DTO for user creation and update operations.
 * Contains user data with validation constraints for ensuring data integrity.
 * This DTO is used in the application layer for user-related operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveUserDto {
    
    /**
     * User ID - null for new users, present for updates
     */
    private Long id;
    
    /**
     * User's full name
     */
    @NotBlank(message = "Name is required and cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name can only contain letters and spaces")
    private String name;
    
    /**
     * User's email address - must be unique in the system
     */
    @NotBlank(message = "Email is required and cannot be empty")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;
    
    /**
     * User's username - must be unique in the system
     */
    @NotBlank(message = "Username is required and cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
    private String username;
    
    /**
     * Additional user metadata for audit purposes
     */
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private boolean active;
    private Instant requestTimestamp;
    
    /**
     * Constructs a SaveUserDto with required fields only.
     * 
     * @param name User's full name
     * @param email User's email address
     * @param username User's unique username
     */
    public SaveUserDto(String name, String email, String username) {
        this.name = name;
        this.email = email;
        this.username = username;
        this.active = true; // Default to active
        this.requestTimestamp = Instant.now();
    }
    
    /**
     * Creates a new SaveUserDto for user creation (without ID).
     * 
     * @param name User's full name
     * @param email User's email address
     * @param username User's unique username
     * @return New SaveUserDto instance for creation
     */
    public static SaveUserDto forCreation(String name, String email, String username) {
        return SaveUserDto.builder()
            .name(name)
            .email(email)
            .username(username)
            .active(true)
            .requestTimestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates a SaveUserDto for user update operations.
     * 
     * @param id Existing user ID
     * @param name Updated name
     * @param email Updated email
     * @param username Updated username
     * @return SaveUserDto instance for update
     */
    public static SaveUserDto forUpdate(Long id, String name, String email, String username) {
        return SaveUserDto.builder()
            .id(id)
            .name(name)
            .email(email)
            .username(username)
            .requestTimestamp(Instant.now())
            .build();
    }
    
    /**
     * Checks if this DTO represents a new user creation or an update.
     * 
     * @return true if this is a new user (ID is null), false if it's an update
     */
    public boolean isNewUser() {
        return this.id == null;
    }
    
    /**
     * Validates that required fields are present and email/username are properly formatted.
     * This method can be called programmatically in addition to annotation-based validation.
     * 
     * @return true if all validations pass, false otherwise
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() && email.contains("@") &&
               username != null && !username.trim().isEmpty() && username.length() >= 3;
    }
    
    /**
     * Creates a display-friendly representation of the user data.
     * 
     * @return String representation suitable for logging or display
     */
    public String toDisplayString() {
        return String.format("User[id=%s, username=%s, email=%s, name=%s]", 
            id, username, email, name);
    }
    
    /**
     * Splits the full name into first and last name components if not already set.
     * This is a utility method for systems that need separate first/last name fields.
     */
    public void splitName() {
        if (firstName == null && lastName == null && name != null && name.contains(" ")) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                firstName = parts[0];
                lastName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
            } else {
                firstName = name.trim();
                lastName = "";
            }
        }
    }
    
    /**
     * Normalizes email to lowercase for consistency.
     */
    public void normalizeEmail() {
        if (email != null) {
            this.email = email.toLowerCase().trim();
        }
    }
    
    /**
     * Normalizes username to lowercase for consistency.
     */
    public void normalizeUsername() {
        if (username != null) {
            this.username = username.toLowerCase().trim();
        }
    }
    
    /**
     * Performs all normalization operations on the DTO data.
     */
    public void normalize() {
        normalizeEmail();
        normalizeUsername();
        splitName();
        if (name != null) {
            this.name = name.trim();
        }
    }
}
