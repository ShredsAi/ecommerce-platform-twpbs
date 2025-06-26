package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortNotificationService;
import ai.shreds.shared.dtos.SharedNotificationRequestDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Infrastructure implementation of the DomainOutputPortNotificationService,
 * sending notifications via HTTP to the external notification service.
 */
@Component
public class InfrastructureNotificationServiceClient implements DomainOutputPortNotificationService {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    @Autowired
    public InfrastructureNotificationServiceClient(RestTemplate restTemplate,
                                                   @Value("${notification.service.url:http://localhost:8084}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    @Override
    @Retry(name = "notification-service")
    public void notifyCustomer(String customerId, String notificationType, Map<String, Object> data) {
        SharedNotificationRequestDTO notification = SharedNotificationRequestDTO.builder()
                .customerId(customerId)
                .type(notificationType)
                .data(data)
                .build();
        HttpEntity<SharedNotificationRequestDTO> entity = buildNotificationRequest(notification);
        sendRequest(entity, "/api/notifications");
    }

    @Override
    @Retry(name = "notification-service")
    public void sendOrderConfirmation(String customerId, String orderId, Map<String, Object> orderDetails) {
        notifyCustomer(customerId, "ORDER_CONFIRMED", orderDetails);
    }

    @Override
    @Retry(name = "notification-service")
    public void sendPaymentConfirmation(String customerId, String orderId, Map<String, Object> paymentDetails) {
        notifyCustomer(customerId, "PAYMENT_CONFIRMED", paymentDetails);
    }

    @Override
    @Retry(name = "notification-service")
    public void sendShippingNotification(String customerId, String orderId, Map<String, Object> shippingDetails) {
        notifyCustomer(customerId, "SHIPPING_UPDATE", shippingDetails);
    }

    @Override
    @Retry(name = "notification-service")
    public void sendDeliveryConfirmation(String customerId, String orderId, Map<String, Object> deliveryDetails) {
        notifyCustomer(customerId, "DELIVERY_CONFIRMED", deliveryDetails);
    }

    @Override
    @Retry(name = "notification-service")
    public void sendCancellationNotification(String customerId, String orderId, String cancellationReason) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("reason", cancellationReason);
        notifyCustomer(customerId, "ORDER_CANCELLED", data);
    }

    private HttpEntity<SharedNotificationRequestDTO> buildNotificationRequest(SharedNotificationRequestDTO notification) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Service-Name", "order-fulfillment");
        headers.add("X-Notification-Type", notification.getType());
        headers.add("X-Priority", notification.getPriority() != null ? notification.getPriority() : "NORMAL");
        return new HttpEntity<>(notification, headers);
    }

    private void sendRequest(HttpEntity<SharedNotificationRequestDTO> entity, String path) {
        try {
            String url = notificationServiceUrl + path;
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (RestClientException e) {
            throw new InfrastructureExternalServiceException("NotificationService", "NOTIFICATION_SEND_FAILED", e);
        }
    }
}