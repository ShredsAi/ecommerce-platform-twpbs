package ai.shreds.domain.ports;

import ai.shreds.shared.dtos.SharedSafetyRuleRequestDTO;
import ai.shreds.shared.dtos.SharedSafetyRuleResponseDTO;

public interface DomainInputPortSafetyStockRule {
    SharedSafetyRuleResponseDTO createOrUpdateRule(SharedSafetyRuleRequestDTO request);
    void deactivateRule(String ruleId);
}