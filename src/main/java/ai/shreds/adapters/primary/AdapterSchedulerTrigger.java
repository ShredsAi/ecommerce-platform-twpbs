package ai.shreds.adapters.primary;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.shreds.application.ports.ApplicationSchedulerInputPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterSchedulerTrigger {

    private final ApplicationSchedulerInputPort schedulerService;

    @Scheduled(cron = "${scheduling.cancellation.timeout}")
    public void timeoutPendingCancellations() {
        try {
            log.info("Starting timeout processing for pending cancellations");
            schedulerService.timeoutPendingCancellations();
            log.info("Completed timeout processing for pending cancellations");
        } catch (Exception e) {
            log.error("Error occurred during timeout processing for pending cancellations", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    @Scheduled(cron = "${scheduling.return.monitor}")
    public void monitorReturnDeadlines() {
        try {
            log.info("Starting return deadline monitoring");
            schedulerService.monitorReturnDeadlines();
            log.info("Completed return deadline monitoring");
        } catch (Exception e) {
            log.error("Error occurred during return deadline monitoring", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }
}
