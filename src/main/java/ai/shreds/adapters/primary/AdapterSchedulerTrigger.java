package ai.shreds.adapters.primary;

import ai.shreds.application.ports.ApplicationSchedulerInputPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AdapterSchedulerTrigger {

    private static final Logger logger = LoggerFactory.getLogger(AdapterSchedulerTrigger.class);
    
    private final ApplicationSchedulerInputPort schedulerService;

    @Autowired
    public AdapterSchedulerTrigger(ApplicationSchedulerInputPort schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(cron = "${scheduling.cancellation.timeout}")
    public void timeoutPendingCancellations() {
        try {
            logger.info("Starting scheduled task: timeout pending cancellations");
            
            schedulerService.timeoutPendingCancellations();
            
            logger.info("Successfully completed scheduled task: timeout pending cancellations");
        } catch (Exception ex) {
            logger.error("Error executing scheduled task: timeout pending cancellations", ex);
        }
    }

    @Scheduled(cron = "${scheduling.return.monitor}")
    public void monitorReturnDeadlines() {
        try {
            logger.info("Starting scheduled task: monitor return deadlines");
            
            schedulerService.monitorReturnDeadlines();
            
            logger.info("Successfully completed scheduled task: monitor return deadlines");
        } catch (Exception ex) {
            logger.error("Error executing scheduled task: monitor return deadlines", ex);
        }
    }
}