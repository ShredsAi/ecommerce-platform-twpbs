package ai.shreds.adapter.exceptions;

import ai.shreds.application.exceptions.ApplicationExceptionWebhookNotFoundException;
import ai.shreds.application.exceptions.ApplicationExceptionWebhookProcessingFailedException;
import ai.shreds.shared.dtos.SharedWebhookErrorResponseDTO;
import ai.shreds.shared.exceptions.SharedExceptionWebhookProcessingException;
import ai.shreds.shared.exceptions.SharedExceptionWebhookValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Global exception handler for webhook-related exceptions in the adapter layer.
 */
@ControllerAdvice
public class AdapterWebhookExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdapterWebhookExceptionHandler.class);

    /**
     * Handles webhook validation exceptions, typically related to signature verification.
     *
     * @param ex The validation exception
     * @return Response with 401 Unauthorized status
     */
    @ExceptionHandler(SharedExceptionWebhookValidationException.class)
    public ResponseEntity<SharedWebhookErrorResponseDTO> handleValidationException(
            SharedExceptionWebhookValidationException ex) {
        log.warn("Webhook validation failed: {} for processor: {}", 
                ex.getReason(), ex.getProcessorType());

        SharedWebhookErrorResponseDTO response = new SharedWebhookErrorResponseDTO();
        response.setError("Webhook validation failed");
        response.setDetails(ex.getReason());
        response.setWebhookId(ex.getWebhookId());
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles webhook processing exceptions that occur during webhook processing.
     *
     * @param ex The processing exception
     * @return Response with 500 Internal Server Error status
     */
    @ExceptionHandler(SharedExceptionWebhookProcessingException.class)
    public ResponseEntity<SharedWebhookErrorResponseDTO> handleProcessingException(
            SharedExceptionWebhookProcessingException ex) {
        log.error("Webhook processing failed: {} for processor: {}", 
                ex.getReason(), ex.getProcessorType(), ex.getCause());

        SharedWebhookErrorResponseDTO response = new SharedWebhookErrorResponseDTO();
        response.setError("Webhook processing failed");
        response.setDetails(ex.getReason());
        response.setWebhookId(ex.getWebhookId());
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles application-specific webhook not found exceptions.
     *
     * @param ex The not found exception
     * @return Response with 404 Not Found status
     */
    @ExceptionHandler(ApplicationExceptionWebhookNotFoundException.class)
    public ResponseEntity<SharedWebhookErrorResponseDTO> handleWebhookNotFoundException(
            ApplicationExceptionWebhookNotFoundException ex) {
        log.warn("Webhook not found: {}", ex.getWebhookId());

        SharedWebhookErrorResponseDTO response = new SharedWebhookErrorResponseDTO();
        response.setError("Webhook not found");
        response.setDetails("The requested webhook could not be found");
        response.setWebhookId(ex.getWebhookId());
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles application-specific webhook processing failures.
     *
     * @param ex The processing failure exception
     * @return Response with 500 Internal Server Error status
     */
    @ExceptionHandler(ApplicationExceptionWebhookProcessingFailedException.class)
    public ResponseEntity<SharedWebhookErrorResponseDTO> handleWebhookProcessingFailedException(
            ApplicationExceptionWebhookProcessingFailedException ex) {
        log.error("Webhook processing failed: {}", ex.getReason(), ex.getCause());

        SharedWebhookErrorResponseDTO response = new SharedWebhookErrorResponseDTO();
        response.setError("Webhook processing failed");
        response.setDetails(ex.getReason());
        response.setWebhookId(ex.getWebhookId());
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles generic exceptions that were not caught by a more specific handler.
     *
     * @param ex The generic exception
     * @return Response with 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SharedWebhookErrorResponseDTO> handleGenericException(
            Exception ex) {
        log.error("Unexpected error during webhook processing", ex);

        SharedWebhookErrorResponseDTO response = new SharedWebhookErrorResponseDTO();
        response.setError("Unexpected error");
        response.setDetails(ex.getMessage());
        response.setWebhookId(UUID.randomUUID()); // No specific webhook ID available
        response.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}