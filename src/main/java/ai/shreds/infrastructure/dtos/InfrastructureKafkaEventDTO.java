package ai.shreds.infrastructure.dtos;

import ai.shreds.domain.entities.DomainEntityPaymentEvent;
import ai.shreds.shared.value_objects.SharedValueMoney;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class InfrastructureKafkaEventDTO {
    private String eventType;
    private String paymentId;
    private String paymentIntentId;
    private String customerId;
    private String orderId;
    private Map<String, Object> amount;
    private String correlationId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private String publishedAt;
    private String webhookId;

    public static InfrastructureKafkaEventDTO fromDomainEntity(DomainEntityPaymentEvent event) {
        Map<String, Object> amountMap = new HashMap<>();
        SharedValueMoney money = event.getAmount();
        amountMap.put("value", money.getAmount());
        amountMap.put("currency", money.getCurrency());

        return InfrastructureKafkaEventDTO.builder()
                .eventType(event.getEventType().name())
                .paymentId(event.getPaymentId().toString())
                .paymentIntentId(event.getPaymentIntentId())
                .customerId(event.getCustomerId().toString())
                .orderId(event.getOrderId().toString())
                .amount(amountMap)
                .correlationId(event.getCorrelationId())
                .publishedAt(event.getPublishedAt().toString())
                .webhookId(event.getWebhookId() != null ? event.getWebhookId().toString() : null)
                .build();
    }
}
