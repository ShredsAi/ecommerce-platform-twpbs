package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationSafetyRuleInputPort;
import ai.shreds.application.ports.ApplicationSafetyStockMonitorInputPort;
import ai.shreds.domain.ports.DomainInputPortSafetyStockRule;
import ai.shreds.domain.ports.DomainInputPortSafetyStockMonitor;
import ai.shreds.shared.dtos.SharedSafetyRuleRequestDTO;
import ai.shreds.shared.dtos.SharedSafetyRuleResponseDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationSafetyRuleService implements ApplicationSafetyRuleInputPort {

    private final DomainInputPortSafetyStockRule domainSafetyRulePort;
    private final DomainInputPortSafetyStockMonitor domainMonitorPort;

    @Override
    @Transactional
    public SharedSafetyRuleResponseDTO createOrUpdateSafetyRule(SharedSafetyRuleRequestDTO request) {
        log.info("Creating/updating safety stock rule for SKU: {} at location: {}",
                request.getSkuId(), request.getLocationId());

        // Create or update the rule through domain service
        SharedSafetyRuleResponseDTO response = domainSafetyRulePort.createOrUpdateRule(request);

        // Trigger immediate evaluation of the new/updated rule
        triggerImmediateEvaluation(request.getSkuId(), request.getLocationId());

        return response;
    }

    private void triggerImmediateEvaluation(String skuId, String locationId) {
        try {
            log.debug("Triggering immediate safety stock evaluation for SKU: {} at location: {}",
                    skuId, locationId);
            
            // Evaluate the specific SKU-Location pair immediately
            domainMonitorPort.evaluateStockLevels();
        } catch (Exception e) {
            // Log but don't fail the rule creation/update if monitoring fails
            log.error("Failed to trigger immediate safety stock evaluation for SKU: {} at location: {}",
                    skuId, locationId, e);
        }
    }
}