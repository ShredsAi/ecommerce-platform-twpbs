package ai.shreds.application.dtos;

import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;

/**
 * DTO representing the result of confirming a payment intent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationPaymentConfirmationDTO {

    private UUID id;
    private String status;
    private UUID paymentId;
    private LocalDateTime processedAt;
    private Boolean requiresAction;
    private Map<String, Object> nextAction;

    /**
     * Builder for ApplicationPaymentConfirmationDTO
     */
    public static ApplicationPaymentConfirmationDTOBuilder builder() {
        return new ApplicationPaymentConfirmationDTOBuilder();
    }

    public static class ApplicationPaymentConfirmationDTOBuilder {
        private UUID id;
        private String status;
        private UUID paymentId;
        private LocalDateTime processedAt;
        private Boolean requiresAction;
        private Map<String, Object> nextAction;

        public ApplicationPaymentConfirmationDTOBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public ApplicationPaymentConfirmationDTOBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ApplicationPaymentConfirmationDTOBuilder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public ApplicationPaymentConfirmationDTOBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public ApplicationPaymentConfirmationDTOBuilder requiresAction(Boolean requiresAction) {
            this.requiresAction = requiresAction;
            return this;
        }

        public ApplicationPaymentConfirmationDTOBuilder nextAction(Map<String, Object> nextAction) {
            this.nextAction = nextAction;
            return this;
        }

        public ApplicationPaymentConfirmationDTO build() {
            return new ApplicationPaymentConfirmationDTO(
                id,
                status,
                paymentId,
                processedAt,
                requiresAction,
                nextAction
            );
        }
    }

    /**
     * Creates an ApplicationPaymentConfirmationDTO from domain entities
     *
     * @param intent The payment intent entity
     * @param payment The payment entity (may be null if 3DS is required)
     * @return The confirmation DTO
     */
    public static ApplicationPaymentConfirmationDTO fromDomainEntity(
            DomainPaymentIntentEntity intent,
            DomainPaymentEntity payment) {

        ApplicationPaymentConfirmationDTOBuilder builder = ApplicationPaymentConfirmationDTO.builder()
            .id(intent.getId().getValue())
            .status(intent.getStatus().name());

        // If a payment exists, set its details
        if (payment != null) {
            builder.paymentId(payment.getId().getValue())
                   .processedAt(payment.getProcessedAt())
                   .requiresAction(false)
                   .nextAction(Map.of());
        } else {
            // If payment is null, this might be a 3DS flow that requires action
            builder.requiresAction(intent.requiresThreeDSecure())
                   .nextAction(new HashMap<>());
        }

        return builder.build();
    }

    /**
     * Creates an ApplicationPaymentConfirmationDTO for 3D Secure flow
     * with the correct nextAction structure that matches the test expectations
     */
    public static ApplicationPaymentConfirmationDTO createForThreeDSecure(
            DomainPaymentIntentEntity intent,
            DomainThreeDSecureEntity threeDSecure) {

        // Create nextAction structure that matches what the test expects
        Map<String, Object> nextAction = new HashMap<>();
        nextAction.put("type", "redirect_to_url");
        
        // Create nested redirect_to_url object with url field
        Map<String, Object> redirectInfo = new HashMap<>();
        redirectInfo.put("url", threeDSecure.getChallengeUrl());
        nextAction.put("redirect_to_url", redirectInfo);

        return ApplicationPaymentConfirmationDTO.builder()
            .id(intent.getId().getValue())
            .status(intent.getStatus().name())
            .requiresAction(true)
            .nextAction(nextAction)
            .build();
    }
}