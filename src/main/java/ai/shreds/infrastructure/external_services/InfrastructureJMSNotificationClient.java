package ai.shreds.infrastructure.external_services;

import ai.shreds.domain.ports.DomainOutputPortNotificationService;
import ai.shreds.infrastructure.exceptions.InfrastructureServiceException;
import ai.shreds.infrastructure.mappers.InfrastructureEventMapper;
import ai.shreds.shared.dtos.SharedNotificationRequestDTO;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InfrastructureJMSNotificationClient implements DomainOutputPortNotificationService {

    private final JmsTemplate jmsTemplate;
    private final InfrastructureEventMapper eventMapper;
    
    private static final String NOTIFICATION_QUEUE = "notification.requests";

    public InfrastructureJMSNotificationClient(JmsTemplate jmsTemplate,
                                              InfrastructureEventMapper eventMapper) {
        this.jmsTemplate = jmsTemplate;
        this.eventMapper = eventMapper;
    }

    @Override
    public void notifyCustomer(String customerId, String notificationType, Map<String, Object> data) {
        try {
            SharedNotificationRequestDTO request = eventMapper.toNotificationRequest(
                    customerId, notificationType, data);
            
            jmsTemplate.convertAndSend(NOTIFICATION_QUEUE, request, message -> {
                message.setStringProperty("customerId", customerId);
                message.setStringProperty("type", notificationType);
                message.setStringProperty("priority", request.getPriority());
                return message;
            });
        } catch (Exception e) {
            throw new InfrastructureServiceException(
                    "Failed to send notification to customer: " + customerId + ", type: " + notificationType,
                    "NOTIFICATION_SERVICE",
                    "JMS_SEND_FAILURE",
                    e
            );
        }
    }
}