package ai.shreds.application.ports;

/**
 * Input port for webhook correlation operations.
 */
public interface ApplicationInputPortCorrelateWebhooks {
    
    /**
     * Correlate pending webhooks with payment records.
     * Called periodically to match previously unmatched webhooks.
     */
    void correlatePendingWebhooks();
    
    /**
     * Reconcile webhooks that remain unmatched after the standard correlation window.
     * Generates alerts and metrics for webhooks exceeding the correlation SLA.
     */
    void reconcileUnmatchedWebhooks();
}