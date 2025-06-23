package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationInputPortCorrelateWebhooks;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for webhook correlation and reconciliation.
 * Executes periodic tasks to process pending webhooks and reconcile unmatched ones.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class AdapterScheduledJobController {

    private final ApplicationInputPortCorrelateWebhooks correlateWebhooksInputPort;
    private final Timer correlationTimer;
    private final Timer reconciliationTimer;

    public AdapterScheduledJobController(
            ApplicationInputPortCorrelateWebhooks correlateWebhooksInputPort,
            MeterRegistry meterRegistry) {
        this.correlateWebhooksInputPort = correlateWebhooksInputPort;
        this.correlationTimer = meterRegistry.timer("webhook.correlation.execution.time");
        this.reconciliationTimer = meterRegistry.timer("webhook.reconciliation.execution.time");
        
        log.info("AdapterScheduledJobController initialized - correlation and reconciliation jobs scheduled");
    }

    /**
     * Correlates pending webhooks with payment records.
     * Runs every 30 seconds to process newly received webhooks that were not matched initially.
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void correlatePendingWebhooks() {
        log.debug("Starting scheduled correlation of pending webhooks");
        Timer.Sample sample = Timer.start();
        
        try {
            correlateWebhooksInputPort.correlatePendingWebhooks();
            log.debug("Completed scheduled correlation of pending webhooks successfully");
            
        } catch (Exception e) {
            log.error("Error during scheduled correlation of pending webhooks - will retry in next cycle", e);
            // Don't rethrow - we want the scheduler to continue running
            
        } finally {
            sample.stop(correlationTimer);
        }
    }

    /**
     * Reconciles unmatched webhooks that have been pending for too long.
     * Runs every 5 minutes to identify webhooks that might need manual intervention.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void reconcileUnmatchedWebhooks() {
        log.debug("Starting reconciliation of unmatched webhooks");
        Timer.Sample sample = Timer.start();
        
        try {
            correlateWebhooksInputPort.reconcileUnmatchedWebhooks();
            log.debug("Completed reconciliation of unmatched webhooks successfully");
            
        } catch (Exception e) {
            log.error("Error during reconciliation of unmatched webhooks - will retry in next cycle", e);
            // Don't rethrow - we want the scheduler to continue running
            
        } finally {
            sample.stop(reconciliationTimer);
        }
    }
}