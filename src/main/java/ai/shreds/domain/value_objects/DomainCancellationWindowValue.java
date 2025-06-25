package ai.shreds.domain.value_objects;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Objects;

/**
 * Domain value object representing the time window in which a cancellation is allowed.
 * Contains business logic for determining if an order can be cancelled based on time constraints.
 */
public class DomainCancellationWindowValue {
    
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Integer hoursBeforeDispatch;
    
    /**
     * Create a cancellation window with start, end, and hours before dispatch.
     */
    public DomainCancellationWindowValue(LocalDateTime startTime, LocalDateTime endTime, Integer hoursBeforeDispatch) {
        this.startTime = Objects.requireNonNull(startTime, "Start time cannot be null");
        this.endTime = Objects.requireNonNull(endTime, "End time cannot be null");
        this.hoursBeforeDispatch = Objects.requireNonNull(hoursBeforeDispatch, "Hours before dispatch cannot be null");
        
        if (hoursBeforeDispatch <= 0) {
            throw new IllegalArgumentException("Hours before dispatch must be positive");
        }
        
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
    }
    
    /**
     * Create a cancellation window with the standard hours before dispatch.
     */
    public DomainCancellationWindowValue(Integer hoursBeforeDispatch) {
        this.startTime = LocalDateTime.now();
        this.endTime = LocalDateTime.now().plusHours(24 * 7); // Default window of 7 days
        this.hoursBeforeDispatch = Objects.requireNonNull(hoursBeforeDispatch, "Hours before dispatch cannot be null");
        
        if (hoursBeforeDispatch <= 0) {
            throw new IllegalArgumentException("Hours before dispatch must be positive");
        }
    }
    
    /**
     * Determine if the current time is within the cancellation window.
     */
    public boolean isWithinWindow(LocalDateTime currentTime) {
        Objects.requireNonNull(currentTime, "Current time cannot be null");
        
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }
    
    /**
     * Calculate the cancellation deadline for an order based on its order date.
     */
    public LocalDateTime calculateDeadline(LocalDateTime orderDate) {
        Objects.requireNonNull(orderDate, "Order date cannot be null");
        
        // The earliest of:
        // 1. The end of the defined window
        // 2. X hours before the expected dispatch time (typically order date + dispatch delay)
        LocalDateTime dispatchTime = calculateEstimatedDispatchTime(orderDate);
        LocalDateTime cancellationDeadline = dispatchTime.minusHours(hoursBeforeDispatch);
        
        // Return the earlier deadline (window end or hours before dispatch)
        if (cancellationDeadline.isAfter(endTime)) {
            return endTime;
        }
        return cancellationDeadline;
    }
    
    /**
     * Estimate when an order will be dispatched based on order date and rules.
     * This is a simplified implementation, real logic would include working days, cut-off times, etc.
     */
    private LocalDateTime calculateEstimatedDispatchTime(LocalDateTime orderDate) {
        // Simple implementation: assume dispatch happens 1 day after order placement
        // Real implementation would include business days, cut-off times, weekends/holidays
        return orderDate.plusDays(1);
    }
    
    /**
     * Check if the cancellation window is still open for an order.
     */
    public boolean isWindowOpenForOrder(LocalDateTime orderDate, LocalDateTime currentTime) {
        LocalDateTime deadline = calculateDeadline(orderDate);
        return !currentTime.isAfter(deadline);
    }
    
    /**
     * Get the remaining hours before cancellation window closes.
     */
    public long getRemainingHours(LocalDateTime orderDate, LocalDateTime currentTime) {
        LocalDateTime deadline = calculateDeadline(orderDate);
        
        if (currentTime.isAfter(deadline)) {
            return 0;
        }
        
        return Duration.between(currentTime, deadline).toHours();
    }
    
    // Getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Integer getHoursBeforeDispatch() { return hoursBeforeDispatch; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainCancellationWindowValue that = (DomainCancellationWindowValue) o;
        return Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(hoursBeforeDispatch, that.hoursBeforeDispatch);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, hoursBeforeDispatch);
    }
    
    @Override
    public String toString() {
        return String.format("DomainCancellationWindowValue{startTime=%s, endTime=%s, hoursBeforeDispatch=%d}", 
                           startTime, endTime, hoursBeforeDispatch);
    }
}