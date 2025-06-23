package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.*;
import ai.shreds.domain.value_objects.*;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import ai.shreds.shared.value_objects.SharedProcessorResponseValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Maps between JPA entities and domain entities.
 * Use this mapper to convert data to/from the persistence layer.
 */
@Component
public class InfrastructureJpaEntityMapper {

    private final ObjectMapper objectMapper;

    public InfrastructureJpaEntityMapper() {
        this.objectMapper = new ObjectMapper();
    }

    // PaymentIntent mappings
    public DomainPaymentIntentEntity toPaymentIntentDomain(InfrastructurePaymentIntentJpaEntity jpa) {
        if (jpa == null) return null;
        
        return new DomainPaymentIntentEntity(
            new DomainPaymentIntentIdValue(jpa.getId()),
            new DomainOrderIdValue(jpa.getOrderId()),
            new DomainCustomerIdValue(jpa.getCustomerId()),
            new DomainMoneyValue(
                BigDecimal.valueOf(jpa.getAmountCents()).divide(BigDecimal.valueOf(100)),
                jpa.getCurrency()
            ),
            DomainPaymentStatusEnum.valueOf(jpa.getStatus()),
            jpa.getPaymentMethodId() != null ? new DomainPaymentMethodIdValue(jpa.getPaymentMethodId()) : null,
            DomainPaymentProcessorTypeEnum.valueOf(jpa.getProcessorType()),
            jpa.getClientSecret(),
            jpa.getExpiresAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getVersion()
        );
    }

    public InfrastructurePaymentIntentJpaEntity toPaymentIntentJpa(DomainPaymentIntentEntity domain) {
        if (domain == null) return null;
        
        InfrastructurePaymentIntentJpaEntity jpa = new InfrastructurePaymentIntentJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setOrderId(domain.getOrderId().getValue());
        jpa.setCustomerId(domain.getCustomerId().getValue());
        jpa.setAmountCents(domain.getAmount().getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        jpa.setCurrency(domain.getAmount().getCurrency());
        jpa.setStatus(domain.getStatus().name());
        jpa.setPaymentMethodId(domain.getPaymentMethodId() != null ? domain.getPaymentMethodId().getValue() : null);
        jpa.setProcessorType(domain.getProcessorType().name());
        jpa.setClientSecret(domain.getClientSecret());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        jpa.setVersion(domain.getVersion());
        return jpa;
    }

    // Payment mappings
    public DomainPaymentEntity toPaymentDomain(InfrastructurePaymentJpaEntity jpa) {
        if (jpa == null) return null;
        
        DomainProcessorResponseValue processorResponse = null;
        if (jpa.getProcessorResponse() != null) {
            Map<String, Object> responseMap = jpa.getProcessorResponse();
            Map<String, Object> metadata = null;
            if (responseMap.containsKey("metadata") && responseMap.get("metadata") instanceof Map) {
                metadata = (Map<String, Object>) responseMap.get("metadata");
            }
            
            processorResponse = new DomainProcessorResponseValue(
                (String) responseMap.getOrDefault("processorId", jpa.getProcessorType()),
                (String) responseMap.getOrDefault("responseCode", "UNKNOWN"),
                (String) responseMap.getOrDefault("responseMessage", "No message"),
                (String) responseMap.getOrDefault("rawResponse", "{}"),
                metadata
            );
        }
        
        // Use the static factory method reconstruct() instead of private constructor
        return DomainPaymentEntity.reconstruct(
            new DomainPaymentIdValue(jpa.getId()),
            new DomainPaymentIntentIdValue(jpa.getPaymentIntentId()),
            new DomainMoneyValue(
                BigDecimal.valueOf(jpa.getAmountCents()).divide(BigDecimal.valueOf(100)),
                jpa.getCurrency()
            ),
            DomainPaymentStatusEnum.valueOf(jpa.getStatus()),
            DomainPaymentProcessorTypeEnum.valueOf(jpa.getProcessorType()),
            processorResponse,
            jpa.getProcessedAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt(),
            jpa.getVersion()
        );
    }

    public InfrastructurePaymentJpaEntity toPaymentJpa(DomainPaymentEntity domain) {
        if (domain == null) return null;
        
        InfrastructurePaymentJpaEntity jpa = new InfrastructurePaymentJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setPaymentIntentId(domain.getPaymentIntentId().getValue());
        jpa.setAmountCents(domain.getAmount().getAmount().multiply(BigDecimal.valueOf(100)).longValue());
        jpa.setCurrency(domain.getAmount().getCurrency());
        jpa.setStatus(domain.getStatus().name());
        jpa.setProcessorType(domain.getProcessorType().name());
        
        if (domain.getProcessorResponse() != null) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("processorId", domain.getProcessorResponse().getProcessorId());
            responseMap.put("responseCode", domain.getProcessorResponse().getResponseCode());
            responseMap.put("responseMessage", domain.getProcessorResponse().getResponseMessage());
            responseMap.put("rawResponse", domain.getProcessorResponse().getRawResponse());
            if (domain.getProcessorResponse().getMetadata() != null) {
                responseMap.put("metadata", domain.getProcessorResponse().getMetadata());
            }
            jpa.setProcessorResponse(responseMap);
        }
        
        jpa.setProcessedAt(domain.getProcessedAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        jpa.setVersion(domain.getVersion());
        return jpa;
    }

    // PaymentMethod mappings
    public DomainPaymentMethodEntity toPaymentMethodDomain(InfrastructurePaymentMethodJpaEntity jpa) {
        if (jpa == null) return null;
        
        DomainPaymentMethodDetailsValue details = null;
        try {
            if (jpa.getDetails() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> detailsMap = objectMapper.readValue(jpa.getDetails(), Map.class);
                details = createPaymentMethodDetails(DomainPaymentMethodTypeEnum.valueOf(jpa.getType()), detailsMap);
            }
        } catch (JsonProcessingException e) {
            // Create minimal details on parsing error
            details = createMinimalPaymentMethodDetails(DomainPaymentMethodTypeEnum.valueOf(jpa.getType()));
        }
        
        return new DomainPaymentMethodEntity(
            new DomainPaymentMethodIdValue(jpa.getId()),
            new DomainCustomerIdValue(jpa.getCustomerId()),
            DomainPaymentMethodTypeEnum.valueOf(jpa.getType()),
            details,
            jpa.getIsDefault(),
            jpa.getIsActive(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }

    public InfrastructurePaymentMethodJpaEntity toPaymentMethodJpa(DomainPaymentMethodEntity domain) {
        if (domain == null) return null;
        
        InfrastructurePaymentMethodJpaEntity jpa = new InfrastructurePaymentMethodJpaEntity();
        jpa.setId(domain.getId().getValue());
        jpa.setCustomerId(domain.getCustomerId().getValue());
        jpa.setType(domain.getType().name());
        jpa.setIsDefault(domain.isDefault());
        jpa.setIsActive(domain.isActive());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        
        if (domain.getDetails() != null) {
            try {
                jpa.setDetails(objectMapper.writeValueAsString(domain.getDetails()));
            } catch (JsonProcessingException e) {
                jpa.setDetails("{}");
            }
        }
        
        return jpa;
    }

    // ThreeDSecure mappings
    public DomainThreeDSecureEntity toThreeDSecureDomain(InfrastructureThreeDSecureJpaEntity jpa) {
        if (jpa == null) return null;
        
        Map<String, Object> authResult = null;
        try {
            if (jpa.getAuthenticationResult() != null) {
                authResult = objectMapper.readValue(
                    jpa.getAuthenticationResult(),
                    new TypeReference<Map<String, Object>>() {}
                );
            }
        } catch (JsonProcessingException e) {
            // Create empty map on parsing error
            authResult = Map.of();
        }
        
        return new DomainThreeDSecureEntity(
            jpa.getId(),
            new DomainPaymentIntentIdValue(jpa.getPaymentIntentId()),
            jpa.getChallengeUrl(),
            DomainThreeDSecureStatusEnum.valueOf(jpa.getStatus()),
            authResult,
            jpa.getExpiresAt(),
            jpa.getCreatedAt(),
            jpa.getUpdatedAt()
        );
    }

    public InfrastructureThreeDSecureJpaEntity toThreeDSecureJpa(DomainThreeDSecureEntity domain) {
        if (domain == null) return null;
        
        InfrastructureThreeDSecureJpaEntity jpa = new InfrastructureThreeDSecureJpaEntity();
        jpa.setId(domain.getId());
        jpa.setPaymentIntentId(domain.getPaymentIntentId().getValue());
        jpa.setChallengeUrl(domain.getChallengeUrl());
        jpa.setStatus(domain.getStatus().name());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        
        if (domain.getAuthenticationResult() != null) {
            try {
                jpa.setAuthenticationResult(objectMapper.writeValueAsString(domain.getAuthenticationResult()));
            } catch (JsonProcessingException e) {
                jpa.setAuthenticationResult("{}");
            }
        }
        
        return jpa;
    }

    // Helper methods
    private DomainPaymentMethodDetailsValue createPaymentMethodDetails(DomainPaymentMethodTypeEnum type, Map<String, Object> detailsMap) {
        switch (type) {
            case CARD:
                // Use static factory method for card details
                return DomainPaymentMethodDetailsValue.forCard(
                    new DomainCardDetailsValue(
                        (String) detailsMap.get("last4"),
                        (String) detailsMap.get("brand"),
                        (Integer) detailsMap.get("expiryMonth"),
                        (Integer) detailsMap.get("expiryYear")
                    )
                );
            case BANK_ACCOUNT:
                // Use static factory method for bank account details
                return DomainPaymentMethodDetailsValue.forBankAccount(
                    new DomainBankAccountDetailsValue(
                        (String) detailsMap.get("last4"),
                        (String) detailsMap.get("bankName"),
                        (String) detailsMap.get("accountType")
                    )
                );
            case DIGITAL_WALLET:
                // Use static factory method for digital wallet details
                return DomainPaymentMethodDetailsValue.forDigitalWallet(
                    new DomainDigitalWalletDetailsValue(
                        (String) detailsMap.get("walletType"),
                        (String) detailsMap.get("email")
                    )
                );
            default:
                return createMinimalPaymentMethodDetails(type);
        }
    }

    private DomainPaymentMethodDetailsValue createMinimalPaymentMethodDetails(DomainPaymentMethodTypeEnum type) {
        // Create minimal details based on type using appropriate static factory methods
        switch (type) {
            case CARD:
                return DomainPaymentMethodDetailsValue.forCard(
                    new DomainCardDetailsValue("0000", "UNKNOWN", 1, 2099)
                );
            case BANK_ACCOUNT:
                return DomainPaymentMethodDetailsValue.forBankAccount(
                    new DomainBankAccountDetailsValue("0000", "UNKNOWN", "UNKNOWN")
                );
            case DIGITAL_WALLET:
                return DomainPaymentMethodDetailsValue.forDigitalWallet(
                    new DomainDigitalWalletDetailsValue("UNKNOWN", "unknown@example.com")
                );
            default:
                return DomainPaymentMethodDetailsValue.forCard(
                    new DomainCardDetailsValue("0000", "UNKNOWN", 1, 2099)
                );
        }
    }
}