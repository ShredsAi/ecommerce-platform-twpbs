package ai.shreds.adapters.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.domain.exceptions.DomainCancellationNotAllowedException;
import ai.shreds.domain.exceptions.DomainReturnNotAllowedException;
import ai.shreds.domain.exceptions.DomainBusinessRuleViolationException;
import ai.shreds.domain.exceptions.DomainInvalidStateTransitionException;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.shared.dtos.SharedReturnResponseDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(basePackages = "ai.shreds.adapters.primary")
public class AdapterExceptionHandler {

    @ExceptionHandler(DomainCancellationNotAllowedException.class)
    public ResponseEntity<SharedCancellationResponseDTO> handleCancellationNotAllowed(DomainCancellationNotAllowedException ex) {
        log.warn("Cancellation not allowed for order {}: {}", ex.getOrderId(), ex.getMessage());
        
        SharedCancellationResponseDTO response = new SharedCancellationResponseDTO();
        response.setCancellationId(null);
        response.setOrderId(ex.getOrderId());
        response.setStatus("REJECTED");
        response.setReason(ex.getReason());
        response.setRequestedAt(LocalDateTime.now());
        response.setRefundAmount(null);
        response.setMessage("Cancellation request rejected: " + ex.getMessage());
        response.setSuccess(false);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(DomainReturnNotAllowedException.class)
    public ResponseEntity<SharedReturnResponseDTO> handleReturnNotAllowed(DomainReturnNotAllowedException ex) {
        log.warn("Return not allowed for order {}: {}", ex.getOrderId(), ex.getMessage());
        
        SharedReturnResponseDTO response = new SharedReturnResponseDTO();
        response.setReturnId(null);
        response.setOrderId(ex.getOrderId());
        response.setRmaNumber(null);
        response.setStatus("REJECTED");
        response.setRequestedAt(LocalDateTime.now());
        response.setReturnInstructions(null);
        response.setReturnAddress(null);
        response.setEstimatedRefund(null);
        response.setSuccess(false);
        response.setMessage("Return request rejected: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(DomainBusinessRuleViolationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleViolation(DomainBusinessRuleViolationException ex) {
        log.warn("Business rule violation - Rule: {}, Details: {}", ex.getRule(), ex.getViolationDetails());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        response.put("error", "Business Rule Violation");
        response.put("message", ex.getMessage());
        response.put("rule", ex.getRule());
        response.put("violationDetails", ex.getViolationDetails());
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(DomainInvalidStateTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidStateTransition(DomainInvalidStateTransitionException ex) {
        log.warn("Invalid state transition for entity {}: {} -> {}", ex.getEntityId(), ex.getCurrentState(), ex.getTargetState());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Invalid State Transition");
        response.put("message", ex.getMessage());
        response.put("entityId", ex.getEntityId());
        response.put("currentState", ex.getCurrentState());
        response.put("targetState", ex.getTargetState());
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AdapterValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(AdapterValidationException ex) {
        log.error("Adapter validation error: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Error");
        response.put("message", ex.getMessage());
        
        if (ex.getFieldName() != null) {
            response.put("field", ex.getFieldName());
            response.put("invalidValue", ex.getInvalidValue());
        }
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(AdapterMessageProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleMessageProcessingException(AdapterMessageProcessingException ex) {
        log.error("Message processing error: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Message Processing Error");
        response.put("message", ex.getMessage());
        
        if (ex.getMessageId() != null) {
            response.put("messageId", ex.getMessageId());
            response.put("messageType", ex.getMessageType());
            response.put("destination", ex.getDestination());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.error("Bean validation error: {}", ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bean Validation Error");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        response.put("fieldErrors", fieldErrors);
        response.put("message", "Validation failed for one or more fields");
        
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error in adapter layer: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
