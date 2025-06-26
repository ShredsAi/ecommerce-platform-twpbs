package ai.shreds.application.dtos;

import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.shared.dtos.SharedPaymentWebhookRequestDTO;
import ai.shreds.shared.value_objects.SharedMoneyValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for payment webhook callbacks in application layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationPaymentWebhookDTO {
    private String transactionId;
    private UUID orderId;
    private String status;
    private String authCode;
    private SharedMoneyValue amount;

    /**
     * Map this DTO to domain payment details entity.
     */
    public DomainPaymentDetailsEntity toDomainPayment() {
        return DomainPaymentDetailsEntity.fromApplicationDTO(this);
    }

    /**
     * Create an application DTO from a shared payment webhook request.
     */
    public static ApplicationPaymentWebhookDTO fromSharedDTO(SharedPaymentWebhookRequestDTO shared) {
        return new ApplicationPaymentWebhookDTO(
            shared.getTransactionId(),
            shared.getOrderId(),
            shared.getStatus(),
            shared.getAuthCode(),
            shared.getAmount()
        );
    }
}
