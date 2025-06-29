package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationStockQueryInputPort;
import ai.shreds.application.ports.ApplicationStockAdjustmentInputPort;
import ai.shreds.application.ports.ApplicationSafetyRuleInputPort;
import ai.shreds.shared.dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Inventory Management", description = "API for managing inventory stock levels, adjustments and safety rules")
public class AdapterInventoryController {

    private final ApplicationStockQueryInputPort applicationStockQueryPort;
    private final ApplicationStockAdjustmentInputPort applicationStockAdjustmentPort;
    private final ApplicationSafetyRuleInputPort applicationSafetyRulePort;

    @GetMapping("/stock/{skuId}/{locationId}")
    @Operation(
        summary = "Get stock levels for SKU at location",
        description = "Retrieves current stock, reserved, and available quantities for a given SKU at specific location"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock levels retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Stock record not found for given SKU and location"),
        @ApiResponse(responseCode = "400", description = "Invalid SKU or location ID format")
    })
    @PreAuthorize("hasRole('INVENTORY_READ') or hasRole('ADMIN')")
    public ResponseEntity<SharedStockLevelDTO> getStock(
            @Parameter(description = "SKU identifier", required = true)
            @PathVariable @NotBlank String skuId,
            @Parameter(description = "Location identifier", required = true)
            @PathVariable @NotBlank String locationId) {
        
        log.debug("Getting stock levels for SKU: {} at location: {}", skuId, locationId);
        SharedStockLevelDTO stockLevel = applicationStockQueryPort.getStock(skuId, locationId);
        log.debug("Retrieved stock level: {}", stockLevel);
        
        return ResponseEntity.ok(stockLevel);
    }

    @PostMapping("/adjust")
    @Operation(
        summary = "Adjust stock quantity",
        description = "Creates a stock adjustment (increase or decrease) with full audit trail and validation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock adjustment completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid adjustment request or business validation failed"),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict - record was modified by another process")
    })
    @PreAuthorize("hasRole('INVENTORY_WRITE') or hasRole('ADMIN')")
    public ResponseEntity<SharedStockAdjustmentResponseDTO> adjustStock(
            @Parameter(description = "Stock adjustment request details", required = true)
            @Valid @RequestBody SharedStockAdjustmentRequestDTO request) {
        
        log.info("Processing stock adjustment for SKU: {} at location: {}, adjustment: {}, reason: {}", 
            request.getSkuId(), request.getLocationId(), request.getAdjustment(), request.getReason());
        
        SharedStockAdjustmentResponseDTO response = applicationStockAdjustmentPort.adjustStock(request);
        
        log.info("Stock adjustment completed successfully. Ledger ID: {}, New quantity: {}", 
            response.getLedgerId(), response.getNewQuantity());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/safety-rules")
    @Operation(
        summary = "Create or update safety stock rule",
        description = "Creates or updates a safety-stock rule that defines minimum allowable quantity for a SKU-Location pair"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Safety rule created successfully"),
        @ApiResponse(responseCode = "200", description = "Safety rule updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid safety rule request data")
    })
    @PreAuthorize("hasRole('INVENTORY_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<SharedSafetyRuleResponseDTO> createSafetyRule(
            @Parameter(description = "Safety rule creation/update request", required = true)
            @Valid @RequestBody SharedSafetyRuleRequestDTO request) {
        
        log.info("Creating/updating safety rule for SKU: {} at location: {}, min quantity: {}", 
            request.getSkuId(), request.getLocationId(), request.getMinQuantity());
        
        SharedSafetyRuleResponseDTO response = applicationSafetyRulePort.createOrUpdateSafetyRule(request);
        
        // Build location URI for created resource
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(response.getRuleId())
            .toUri();
        
        log.info("Safety rule processed successfully. Rule ID: {}, Active: {}", 
            response.getRuleId(), response.getIsActive());
        
        // Return 201 for new rules, 200 for updates
        HttpStatus status = response.getRuleId() != null ? HttpStatus.CREATED : HttpStatus.OK;
        
        return ResponseEntity.status(status)
            .location(location)
            .body(response);
    }
    
    @GetMapping("/safety-rules/{skuId}/{locationId}")
    @Operation(
        summary = "Get safety stock rule",
        description = "Retrieves the safety stock rule for a specific SKU-Location pair"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Safety rule retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Safety rule not found for given SKU and location")
    })
    @PreAuthorize("hasRole('INVENTORY_READ') or hasRole('ADMIN')")
    public ResponseEntity<SharedSafetyRuleResponseDTO> getSafetyRule(
            @Parameter(description = "SKU identifier", required = true)
            @PathVariable @NotBlank String skuId,
            @Parameter(description = "Location identifier", required = true)
            @PathVariable @NotBlank String locationId) {
        
        log.debug("Getting safety rule for SKU: {} at location: {}", skuId, locationId);
        // This would require implementing a get method in the application service
        // For now, we'll return a method not implemented response
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}