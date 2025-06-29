package ai.shreds.adapter.exceptions;

import ai.shreds.application.exceptions.*;
import ai.shreds.domain.exceptions.*;
import ai.shreds.infrastructure.exceptions.*;
import ai.shreds.shared.dtos.SharedErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API controllers in the adapter layer.
 * Provides consistent error responses and proper HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class AdapterExceptionGlobalExceptionHandler {

    /**
     * Handles validation errors from request body validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SharedErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        String errorMessage = "Validation failed: " + fieldErrors.toString();
        log.warn("Validation error: {}", errorMessage);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "VALIDATION_ERROR",
            errorMessage,
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles constraint violation errors from path variable validation.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<SharedErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        String errorMessage = violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("Constraint violation: {}", errorMessage);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Constraint violation: " + errorMessage,
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles method argument type mismatch errors (e.g., invalid path parameters).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<SharedErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String errorMessage = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
            ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        log.warn("Type mismatch error: {}", errorMessage);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "TYPE_MISMATCH",
            errorMessage,
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles malformed JSON request body errors.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SharedErrorResponse> handleMalformedJson(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        log.warn("Malformed JSON request: {}", ex.getMessage());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "MALFORMED_JSON",
            "Invalid JSON format in request body",
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles access denied errors (security).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SharedErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "ACCESS_DENIED",
            "Insufficient privileges to access this resource",
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handles application layer exceptions.
     */
    @ExceptionHandler(ApplicationExceptionStockNotFoundException.class)
    public ResponseEntity<SharedErrorResponse> handleStockNotFound(
            ApplicationExceptionStockNotFoundException ex, WebRequest request) {
        
        log.warn("Stock not found: SKU={}, Location={}", ex.getSkuId(), ex.getLocationId());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "STOCK_NOT_FOUND",
            String.format("Stock not found for SKU '%s' at location '%s'", ex.getSkuId(), ex.getLocationId()),
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handles insufficient stock exceptions.
     */
    @ExceptionHandler(ApplicationExceptionInsufficientStockException.class)
    public ResponseEntity<SharedErrorResponse> handleInsufficientStock(
            ApplicationExceptionInsufficientStockException ex, WebRequest request) {
        
        log.warn("Insufficient stock: requested={}, available={}", ex.getRequestedQuantity(), ex.getAvailableQuantity());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "INSUFFICIENT_STOCK",
            String.format("Insufficient stock: requested %s, available %s", 
                ex.getRequestedQuantity(), ex.getAvailableQuantity()),
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handles optimistic locking exceptions.
     */
    @ExceptionHandler(ApplicationExceptionOptimisticLockException.class)
    public ResponseEntity<SharedErrorResponse> handleOptimisticLock(
            ApplicationExceptionOptimisticLockException ex, WebRequest request) {
        
        log.warn("Optimistic lock exception: {}", ex.getMessage());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "OPTIMISTIC_LOCK_ERROR",
            "The resource was modified by another process. Please retry your operation.",
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handles domain exceptions.
     */
    @ExceptionHandler(DomainExceptionEntityNotFound.class)
    public ResponseEntity<SharedErrorResponse> handleEntityNotFound(
            DomainExceptionEntityNotFound ex, WebRequest request) {
        
        log.warn("Entity not found: type={}, id={}", ex.getEntityType(), ex.getId());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "ENTITY_NOT_FOUND",
            String.format("%s not found with ID: %s", ex.getEntityType(), ex.getId()),
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handles domain invariant violations.
     */
    @ExceptionHandler(DomainExceptionInvariantViolation.class)
    public ResponseEntity<SharedErrorResponse> handleInvariantViolation(
            DomainExceptionInvariantViolation ex, WebRequest request) {
        
        log.warn("Domain invariant violation: {}", ex.getMessage());
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "BUSINESS_RULE_VIOLATION",
            "Business rule violation: " + ex.getMessage(),
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles infrastructure exceptions.
     */
    @ExceptionHandler(InfrastructureExceptionDatabaseError.class)
    public ResponseEntity<SharedErrorResponse> handleDatabaseError(
            InfrastructureExceptionDatabaseError ex, WebRequest request) {
        
        log.error("Database error: {}", ex.getMessage(), ex);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "DATABASE_ERROR",
            "A database error occurred. Please try again later.",
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handles external service exceptions.
     */
    @ExceptionHandler(InfrastructureExceptionExternalServiceError.class)
    public ResponseEntity<SharedErrorResponse> handleExternalServiceError(
            InfrastructureExceptionExternalServiceError ex, WebRequest request) {
        
        log.error("External service error [{}]: {}", ex.getServiceName(), ex.getMessage(), ex);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "EXTERNAL_SERVICE_ERROR",
            String.format("External service '%s' is temporarily unavailable. Please try again later.", ex.getServiceName()),
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SharedErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        SharedErrorResponse errorResponse = new SharedErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support if the problem persists.",
            Instant.now(),
            request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}