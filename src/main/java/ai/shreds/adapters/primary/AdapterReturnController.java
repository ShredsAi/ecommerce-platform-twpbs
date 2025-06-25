package ai.shreds.adapters.primary;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.shared.value_objects.SharedReturnRequestParams;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import ai.shreds.shared.dtos.SharedReturnResponseDTO;
import ai.shreds.adapters.exceptions.AdapterValidationException;

@Slf4j
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class AdapterReturnController {

    private final ApplicationReturnInputPort returnService;

    @PostMapping
    public ResponseEntity<SharedReturnResponseDTO> requestReturn(
            @Valid @RequestBody SharedReturnRequestParams params) {
        
        log.info("Received return request for order: {}", params.orderId());
        
        try {
            // Additional validation if needed
            if (params.orderId() == null || params.orderId().trim().isEmpty()) {
                throw new AdapterValidationException("Order ID is required", "orderId", params.orderId());
            }
            
            if (params.items() == null || params.items().isEmpty()) {
                throw new AdapterValidationException("At least one item must be specified for return", "items", params.items());
            }
            
            SharedReturnResponseDTO response = returnService.requestReturn(params);
            log.info("Return request processed successfully for order: {}, return ID: {}", 
                    params.orderId(), response.returnId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error processing return request for order: {}", params.orderId(), ex);
            throw ex;
        }
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<SharedReturnResponseDTO> getReturn(@PathVariable String returnId) {
        
        log.debug("Retrieving return: {}", returnId);
        
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new AdapterValidationException("Return ID is required", "returnId", returnId);
        }
        
        SharedReturnResponseDTO response = returnService.getReturn(returnId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<SharedReturnResponseDTO>> getReturnsByOrder(@PathVariable String orderId) {
        
        log.debug("Retrieving returns for order: {}", orderId);
        
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new AdapterValidationException("Order ID is required", "orderId", orderId);
        }
        
        List<SharedReturnResponseDTO> responses = returnService.getReturnsByOrder(orderId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{returnId}/status")
    public ResponseEntity<SharedReturnResponseDTO> updateReturnStatus(
            @PathVariable String returnId,
            @RequestParam("status") SharedReturnStatusEnum status) {
        
        log.info("Updating return status for return: {} to status: {}", returnId, status);
        
        if (returnId == null || returnId.trim().isEmpty()) {
            throw new AdapterValidationException("Return ID is required", "returnId", returnId);
        }
        
        if (status == null) {
            throw new AdapterValidationException("Status is required", "status", null);
        }
        
        try {
            SharedReturnResponseDTO response = returnService.updateReturnStatus(returnId, status);
            log.info("Return status updated successfully for return: {}", returnId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error updating return status for return: {}", returnId, ex);
            throw ex;
        }
    }
}
