package ai.shreds.infrastructure.external_services;

import ai.shreds.application.ports.ApplicationNotificationOutputPort;
import ai.shreds.shared.dtos.SharedNotificationDTO;
import ai.shreds.infrastructure.exceptions.InfrastructureExternalServiceException;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for Notification Service, implements retry for sending notifications.
 */
@Service
public class InfrastructureNotificationServiceClient implements ApplicationNotificationOutputPort {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public InfrastructureNotificationServiceClient(RestTemplate restTemplate,
                                                   @Value("${notification.service.url}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    @Override
    @Retry(name = "notification-service")
    public void sendNotification(SharedNotificationDTO notification) {
        try {
            HttpEntity<SharedNotificationDTO> request = buildNotificationRequest(notification);
            restTemplate.postForEntity(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("NotificationService", ex.getMessage(), ex);
        }
    }

    @Override
    public void sendBulkNotifications(List<SharedNotificationDTO> notifications) {
        try {
            HttpEntity<List<SharedNotificationDTO>> request = new HttpEntity<>(notifications);
            restTemplate.postForEntity(notificationServiceUrl + "/notifications/bulk", request, Void.class);
        } catch (Exception ex) {
            throw new InfrastructureExternalServiceException("NotificationService", ex.getMessage(), ex);
        }
    }

    private HttpEntity<SharedNotificationDTO> buildNotificationRequest(SharedNotificationDTO notification) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(notification, headers);
    }
}