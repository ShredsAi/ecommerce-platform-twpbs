package ai.shreds.application.ports;

/**
 * Input port for scheduled tasks in the application layer.
 */
public interface ApplicationSchedulerInputPort {
    /**
     * Timeout any pending cancellation requests that have exceeded their allowed window.
     */
    void timeoutPendingCancellations();

    /**
     * Monitor and process any return deadlines as part of scheduled operations.
     */
    void monitorReturnDeadlines();
}