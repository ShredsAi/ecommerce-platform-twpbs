package ai.shreds.adapters.primary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.shared.value_objects.SharedCancellationRequestParams;
import ai.shreds.shared.dtos.SharedCancellationResponseDTO;
import ai.shreds.adapters.exceptions.AdapterValidationException;

@Slf4j
@RestController
@RequestMapping("/api/cancellations")
@RequiredArgsConstructor
public class AdapterCancellationController {

    private final ApplicationCancellationInputPort cancellationService;

    @PostMapping
    public ResponseEntity<SharedCancellationResponseDTO> requestCancellation(
            @Valid @RequestBody SharedCancellationRequestParams params) {
        
        log.info("Received cancellation request for order: {}", params.orderId());
        
        try {
            // Additional validation if needed
            if (params.orderId() == null || params.orderId().trim().isEmpty()) {
                throw new AdapterValidationException("Order ID is required", "orderId", params.orderId());
            }
            
            SharedCancellationResponseDTO response = cancellationService.requestCancellation(params);
            log.info("Cancellation request processed successfully for order: {}, cancellation ID: {}", 
                    params.orderId(), response.getCancellationId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error processing cancellation request for order: {}", params.orderId(), ex);
            throw ex;
        }
    }

    @GetMapping("/{cancellationId}")
    public ResponseEntity<SharedCancellationResponseDTO> getCancellation(@PathVariable String cancellationId) {
        
        log.debug("Retrieving cancellation: {}", cancellationId);
        
        if (cancellationId == null || cancellationId.trim().isEmpty()) {
            throw new AdapterValidationException("Cancellation ID is required", "cancellationId", cancellationId);
        }
        
        SharedCancellationResponseDTO response = cancellationService.getCancellation(cancellationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<SharedCancellationResponseDTO>> getCancellationsByOrder(@PathVariable String orderId) {
        
        log.debug("Retrieving cancellations for order: {}", orderId);
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new AdapterValidationException("Order ID is required", "orderId", orderId);
        }
        
        List<SharedCancellationResponseDTO> responses = cancellationService.getCancellationsByOrder(orderId);
        return ResponseEntity.ok(responses);
    }
}