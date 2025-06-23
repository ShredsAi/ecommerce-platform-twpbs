package ai.shreds.adapter.primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ai.shreds.application.ports.ApplicationProcessWebhookInputPort;
import ai.shreds.shared.dtos.SharedPaymentWebhookProcessedEvent;

@Component
public class AdapterPaymentWebhookEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AdapterPaymentWebhookEventListener.class);
    
    private final ApplicationProcessWebhookInputPort applicationProcessWebhookPort;

    public AdapterPaymentWebhookEventListener(ApplicationProcessWebhookInputPort applicationProcessWebhookPort) {
        this.applicationProcessWebhookPort = applicationProcessWebhookPort;
    }

    @EventListener
    @Async
    public void onPaymentWebhookProcessed(SharedPaymentWebhookProcessedEvent event) {
        logger.info("Received webhook event for payment: {} with status: {}", event.getPaymentId(), event.getNewStatus());
        try {
            applicationProcessWebhookPort.processWebhookUpdate(event.toApplicationDTO());
            logger.info("Successfully processed webhook event for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            logger.error("Error processing webhook event for payment: " + event.getPaymentId(), e);
            throw e;
        }
    }
}