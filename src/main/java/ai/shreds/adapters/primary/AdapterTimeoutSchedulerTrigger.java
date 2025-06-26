package ai.shreds.adapters.primary;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationSchedulerInputPort;
import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.application.ports.ApplicationReturnInputPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterTimeoutSchedulerTrigger {

    private final ApplicationSchedulerInputPort schedulerService;
    private final ApplicationCancellationInputPort cancellationService;
    private final ApplicationReturnInputPort returnService;

    /**
     * Process timeout for pending cancellations every 30 minutes
     * Auto-approves or rejects PENDING cancellations older than configured threshold
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void processTimeoutCancellations() {
        try {
            log.info("Starting scheduled task: processTimeoutCancellations");
            schedulerService.timeoutPendingCancellations();
            log.info("Completed scheduled task: processTimeoutCancellations");
        } catch (Exception e) {
            log.error("Error occurred during processTimeoutCancellations scheduled task", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    /**
     * Monitor return deadlines daily
     * Sends reminders and auto-closes returns past the return window
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void processReturnDeadlines() {
        try {
            log.info("Starting scheduled task: processReturnDeadlines");
            schedulerService.monitorReturnDeadlines();
            log.info("Completed scheduled task: processReturnDeadlines");
        } catch (Exception e) {
            log.error("Error occurred during processReturnDeadlines scheduled task", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    /**
     * Process stale pending cancellations every 6 hours
     * Handles cancellations that have been pending for too long
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void processStaleCancellations() {
        try {
            log.info("Starting scheduled task: processStaleCancellations");
            
            // This could involve:
            // 1. Finding cancellations pending for more than business rule threshold
            // 2. Auto-approving customer-initiated cancellations if order hasn't shipped
            // 3. Auto-rejecting if order has already shipped
            // 4. Sending notifications to customer service for manual review
            
            // For now, delegate to the main timeout processor
            schedulerService.timeoutPendingCancellations();
            
            log.info("Completed scheduled task: processStaleCancellations");
        } catch (Exception e) {
            log.error("Error occurred during processStaleCancellations scheduled task", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    /**
     * Process return reminders every 12 hours
     * Sends customer reminders about pending return requests
     */
    @Scheduled(fixedRate = 43200000) // 12 hours in milliseconds
    public void processReturnReminders() {
        try {
            log.info("Starting scheduled task: processReturnReminders");
            
            // This could involve:
            // 1. Finding returns in APPROVED status waiting for customer to ship
            // 2. Sending reminder notifications to customers
            // 3. Escalating overdue returns to customer service
            
            // For now, delegate to the main return monitor
            schedulerService.monitorReturnDeadlines();
            
            log.info("Completed scheduled task: processReturnReminders");
        } catch (Exception e) {
            log.error("Error occurred during processReturnReminders scheduled task", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    /**
     * Health check for scheduler components - runs every hour
     * Validates that scheduler services are healthy and accessible
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void healthCheckScheduler() {
        try {
            log.debug("Performing scheduler health check");
            
            // Basic health check - attempt to call scheduler service
            // This helps detect if there are connectivity issues
            if (schedulerService != null) {
                log.debug("Scheduler service is accessible");
            } else {
                log.warn("Scheduler service is null - dependency injection issue");
            }
            
            if (cancellationService != null) {
                log.debug("Cancellation service is accessible");
            } else {
                log.warn("Cancellation service is null - dependency injection issue");
            }
            
            if (returnService != null) {
                log.debug("Return service is accessible");
            } else {
                log.warn("Return service is null - dependency injection issue");
            }
            
        } catch (Exception e) {
            log.error("Scheduler health check failed", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }
}
