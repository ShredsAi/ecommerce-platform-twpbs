package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationSchedulerInputPort;
import ai.shreds.application.ports.ApplicationCancellationInputPort;
import ai.shreds.application.ports.ApplicationReturnInputPort;
import ai.shreds.application.exceptions.ApplicationTransactionalException;
import ai.shreds.domain.ports.DomainOutputPortCancellationRepository;
import ai.shreds.domain.ports.DomainOutputPortReturnRepository;
import ai.shreds.domain.entities.DomainCancellationRequestEntity;
import ai.shreds.domain.entities.DomainReturnRequestEntity;
import ai.shreds.shared.enums.SharedCancellationStatusEnum;
import ai.shreds.shared.enums.SharedReturnStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for handling scheduled tasks related to cancellations and returns.
 * Implements timeout processing and deadline monitoring with proper error handling.
 */
@Slf4j
@Service
public class ApplicationSchedulerService implements ApplicationSchedulerInputPort {

    private final ApplicationCancellationInputPort cancellationService;
    private final ApplicationReturnInputPort returnService;
    private final DomainOutputPortCancellationRepository cancellationRepository;
    private final DomainOutputPortReturnRepository returnRepository;

    public ApplicationSchedulerService(
            ApplicationCancellationInputPort cancellationService,
            ApplicationReturnInputPort returnService,
            DomainOutputPortCancellationRepository cancellationRepository,
            DomainOutputPortReturnRepository returnRepository) {
        this.cancellationService = cancellationService;
        this.returnService = returnService;
        this.cancellationRepository = cancellationRepository;
        this.returnRepository = returnRepository;
    }

    /**
     * Scheduled task to process pending cancellations that have exceeded timeout.
     * Runs every 30 minutes as per business requirements.
     */
    @Override
    @Scheduled(cron = "0 */30 * * * ?") // Every 30 minutes
    public void timeoutPendingCancellations() {
        log.info("Starting timeout processing for pending cancellations");
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            // Business rule: 2 hours timeout for customer cancellations
            LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
            
            List<DomainCancellationRequestEntity> pendingCancellations = 
                cancellationRepository.findPendingBefore(cutoff);
            
            log.info("Found {} pending cancellations to timeout", pendingCancellations.size());
            
            for (DomainCancellationRequestEntity cancellation : pendingCancellations) {
                try {
                    log.debug("Processing timeout for cancellation: {} (order: {})", 
                        cancellation.getCancellationId(), cancellation.getOrderId());
                    
                    // Check if cancellation is still in PENDING status
                    if (SharedCancellationStatusEnum.PENDING.equals(cancellation.getStatus())) {
                        // Auto-approve and complete the cancellation due to timeout
                        cancellationService.completeCancellation(cancellation.getCancellationId());
                        processedCount.incrementAndGet();
                        
                        log.info("Successfully timed out and completed cancellation: {}", 
                            cancellation.getCancellationId());
                    } else {
                        log.debug("Skipping cancellation {} - no longer in PENDING status: {}", 
                            cancellation.getCancellationId(), cancellation.getStatus());
                    }
                    
                } catch (ApplicationTransactionalException ex) {
                    log.error("Failed to timeout cancellation: {} - {}", 
                        cancellation.getCancellationId(), ex.getMessage());
                    errorCount.incrementAndGet();
                } catch (Exception ex) {
                    log.error("Unexpected error processing cancellation timeout: {}", 
                        cancellation.getCancellationId(), ex);
                    errorCount.incrementAndGet();
                }
            }
            
            log.info("Completed timeout processing - Processed: {}, Errors: {}", 
                processedCount.get(), errorCount.get());
                
        } catch (Exception ex) {
            log.error("Critical error in timeout processing job", ex);
            throw new ApplicationTransactionalException(
                "Failed to execute timeout processing: " + ex.getMessage(),
                null,
                "timeoutPendingCancellations"
            );
        }
    }

    /**
     * Scheduled task to monitor return deadlines and close expired returns.
     * Runs daily at 2 AM as per business requirements.
     */
    @Override
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void monitorReturnDeadlines() {
        log.info("Starting return deadline monitoring");
        
        AtomicInteger closedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            // Get all returns in various statuses that might need deadline processing
            List<SharedReturnStatusEnum> statusesToCheck = List.of(
                SharedReturnStatusEnum.REQUESTED,
                SharedReturnStatusEnum.APPROVED,
                SharedReturnStatusEnum.IN_TRANSIT,
                SharedReturnStatusEnum.RECEIVED
            );
            
            LocalDateTime now = LocalDateTime.now();
            int totalReturnsChecked = 0;
            
            for (SharedReturnStatusEnum status : statusesToCheck) {
                try {
                    List<DomainReturnRequestEntity> returns = returnRepository.findByReturnStatus(status);
                    totalReturnsChecked += returns.size();
                    
                    log.debug("Checking {} returns in status: {}", returns.size(), status);
                    
                    for (DomainReturnRequestEntity returnEntity : returns) {
                        try {
                            boolean shouldClose = shouldCloseReturn(returnEntity, now);
                            
                            if (shouldClose) {
                                log.info("Closing expired return: {} (RMA: {})", 
                                    returnEntity.getReturnId(), returnEntity.getRmaNumber());
                                
                                returnService.updateReturnStatus(returnEntity.getReturnId(), SharedReturnStatusEnum.CLOSED);
                                closedCount.incrementAndGet();
                            }
                            
                        } catch (ApplicationTransactionalException ex) {
                            log.error("Failed to close expired return: {} - {}", 
                                returnEntity.getReturnId(), ex.getMessage());
                            errorCount.incrementAndGet();
                        } catch (Exception ex) {
                            log.error("Unexpected error processing return deadline: {}", 
                                returnEntity.getReturnId(), ex);
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error processing returns with status: {}", status, ex);
                    errorCount.incrementAndGet();
                }
            }
            
            log.info("Completed return deadline monitoring - Checked: {}, Closed: {}, Errors: {}", 
                totalReturnsChecked, closedCount.get(), errorCount.get());
                
        } catch (Exception ex) {
            log.error("Critical error in return deadline monitoring job", ex);
            throw new ApplicationTransactionalException(
                "Failed to execute return deadline monitoring: " + ex.getMessage(),
                null,
                "monitorReturnDeadlines"
            );
        }
    }

    /**
     * Determines if a return should be closed based on business rules.
     */
    private boolean shouldCloseReturn(DomainReturnRequestEntity returnEntity, LocalDateTime now) {
        // Business rule: Returns expire 30 days after request
        // Extended to 45 days during holiday season (would need configuration)
        LocalDateTime requestDate = returnEntity.getRequestedAt();
        if (requestDate == null) {
            log.warn("Return {} has null request date, skipping", returnEntity.getReturnId());
            return false;
        }
        
        int standardDays = 30;
        int holidayExtensionDays = 15; // Total 45 days during holidays
        
        // TODO: Implement holiday period detection from configuration
        boolean isHolidayPeriod = isHolidayPeriod(now);
        int effectiveDays = isHolidayPeriod ? standardDays + holidayExtensionDays : standardDays;
        
        LocalDateTime deadline = requestDate.plusDays(effectiveDays);
        boolean shouldClose = now.isAfter(deadline);
        
        if (shouldClose) {
            log.debug("Return {} expires: requested={}, deadline={}, now={}, holidayPeriod={}", 
                returnEntity.getReturnId(), requestDate, deadline, now, isHolidayPeriod);
        }
        
        return shouldClose;
    }

    /**
     * Determines if current date falls within holiday period.
     * This is a simplified implementation - in real system this would be configurable.
     */
    private boolean isHolidayPeriod(LocalDateTime date) {
        // Simple implementation: Consider November-December as holiday period
        int month = date.getMonthValue();
        return month == 11 || month == 12;
    }

    /**
     * Additional scheduled method for system health monitoring.
     * Runs every hour to log system status.
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    public void logSystemHealth() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            
            // Count pending cancellations
            List<DomainCancellationRequestEntity> pendingCancellations = 
                cancellationRepository.findByCancellationStatus(SharedCancellationStatusEnum.PENDING);
            
            // Count active returns
            List<DomainReturnRequestEntity> activeReturns = 
                returnRepository.findByReturnStatus(SharedReturnStatusEnum.REQUESTED);
            
            log.info("System Health - Pending Cancellations: {}, Active Returns: {}", 
                pendingCancellations.size(), activeReturns.size());
                
        } catch (Exception ex) {
            log.error("Error in system health monitoring", ex);
        }
    }
}