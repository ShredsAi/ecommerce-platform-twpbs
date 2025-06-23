package ai.shreds.adapter.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import ai.shreds.application.exceptions.ApplicationPaymentValidationException;
import ai.shreds.application.exceptions.ApplicationPaymentNotFoundException;
import ai.shreds.application.exceptions.ApplicationPaymentProcessingException;
import ai.shreds.shared.dtos.SharedErrorResponse;

@RestControllerAdvice
public class AdapterRestExceptionHandler {

    @ExceptionHandler(ApplicationPaymentValidationException.class)
    public ResponseEntity<SharedErrorResponse> handleValidationException(ApplicationPaymentValidationException ex) {
        SharedErrorResponse error = new SharedErrorResponse();
        error.setError("VALIDATION_ERROR");
        error.setMessage(ex.getMessage());
        error.setDetails(ex.getValidationErrors());
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(ApplicationPaymentNotFoundException.class)
    public ResponseEntity<SharedErrorResponse> handleNotFoundException(ApplicationPaymentNotFoundException ex) {
        SharedErrorResponse error = new SharedErrorResponse();
        error.setError("NOT_FOUND");
        error.setMessage(ex.getMessage());
        error.setDetails(List.of("Payment ID: " + ex.getPaymentId()));
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(ApplicationPaymentProcessingException.class)
    public ResponseEntity<SharedErrorResponse> handlePaymentException(ApplicationPaymentProcessingException ex) {
        SharedErrorResponse error = new SharedErrorResponse();
        error.setError("PAYMENT_ERROR");
        error.setMessage(ex.getMessage());
        error.setDeclineCode(ex.getProcessorError());
        error.setDetails(List.of("Retryable: " + ex.isRetryable()));
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SharedErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());
        
        SharedErrorResponse error = new SharedErrorResponse();
        error.setError("VALIDATION_ERROR");
        error.setMessage("Validation failed for request parameters");
        error.setDetails(errors);
        error.setTimestamp(LocalDateTime.now());
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SharedErrorResponse> handleGenericException(Exception ex) {
        SharedErrorResponse error = new SharedErrorResponse();
        error.setError("INTERNAL_SERVER_ERROR");
        error.setMessage("An unexpected error occurred");
        error.setDetails(List.of(ex.getMessage()));
        error.setTimestamp(LocalDateTime.now());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}
