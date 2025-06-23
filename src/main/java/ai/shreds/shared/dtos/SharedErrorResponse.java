package ai.shreds.shared.dtos;

import lombok.*;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Generic error response DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedErrorResponse {
    private String error;
    private String message;
    private List<String> details;
    private String declineCode;
    private LocalDateTime timestamp;
}