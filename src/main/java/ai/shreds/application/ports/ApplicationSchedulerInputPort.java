package ai.shreds.application.ports;

public interface ApplicationSchedulerInputPort {

    /**
     * Scan and process pending cancellations that have timed out.
     */
    void timeoutPendingCancellations();

    /**
     * Monitor and process return requests that exceed their deadline.
     */
    void monitorReturnDeadlines();
}
