package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedSafetyRuleRequestDTO;
import ai.shreds.shared.dtos.SharedSafetyRuleResponseDTO;

public interface ApplicationSafetyRuleInputPort {
    /**
     * Creates or updates a safety stock rule for a specific SKU at a specific location
     *
     * @param request The safety rule request containing SKU, location and minimum quantity
     * @return Response containing the created or updated safety rule
     */
    SharedSafetyRuleResponseDTO createOrUpdateSafetyRule(SharedSafetyRuleRequestDTO request);
}