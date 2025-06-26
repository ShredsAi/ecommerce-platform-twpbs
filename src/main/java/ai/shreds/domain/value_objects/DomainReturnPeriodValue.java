package ai.shreds.domain.value_objects;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Domain value object representing the time period during which an order can be returned.
 * Contains business logic for determining if a return is allowed based on time constraints.
 */
public class DomainReturnPeriodValue {
    
    private final Integer standardDays;
    private final Integer holidayExtensionDays;
    private final boolean isHolidayPeriod;
    
    // Standard return period in days (usually 30 days)
    public static final int DEFAULT_STANDARD_DAYS = 30;
    
    // Holiday extension period in days (usually 15 days added)
    public static final int DEFAULT_HOLIDAY_EXTENSION = 15;
    
    /**
     * Create a return period with standard days, holiday extension, and holiday flag.
     */
    public DomainReturnPeriodValue(Integer standardDays, Integer holidayExtensionDays, boolean isHolidayPeriod) {
        this.standardDays = Objects.requireNonNull(standardDays, "Standard days cannot be null");
        this.holidayExtensionDays = Objects.requireNonNull(holidayExtensionDays, "Holiday extension days cannot be null");
        this.isHolidayPeriod = isHolidayPeriod;
        
        if (standardDays <= 0) {
            throw new IllegalArgumentException("Standard days must be positive");
        }
        
        if (holidayExtensionDays < 0) {
            throw new IllegalArgumentException("Holiday extension days cannot be negative");
        }
    }
    
    /**
     * Create a return period with default values and holiday flag.
     */
    public DomainReturnPeriodValue(boolean isHolidayPeriod) {
        this(DEFAULT_STANDARD_DAYS, DEFAULT_HOLIDAY_EXTENSION, isHolidayPeriod);
    }
    
    /**
     * Create a return period with standard days and holiday flag.
     */
    public DomainReturnPeriodValue(Integer standardDays, boolean isHolidayPeriod) {
        this(standardDays, DEFAULT_HOLIDAY_EXTENSION, isHolidayPeriod);
    }
    
    /**
     * Determine if the current time is within the return period for an item delivered on the given date.
     */
    public boolean isWithinReturnPeriod(LocalDateTime deliveryDate) {
        Objects.requireNonNull(deliveryDate, "Delivery date cannot be null");
        
        int effectiveDays = getEffectiveDays();
        LocalDateTime returnDeadline = deliveryDate.plusDays(effectiveDays);
        
        return LocalDateTime.now().isBefore(returnDeadline);
    }
    
    /**
     * Get the total number of days in the return period, including holiday extension if applicable.
     */
    public int getEffectiveDays() {
        return standardDays + (isHolidayPeriod ? holidayExtensionDays : 0);
    }
    
    /**
     * Calculate the return deadline for an item delivered on the given date.
     */
    public LocalDateTime calculateReturnDeadline(LocalDateTime deliveryDate) {
        Objects.requireNonNull(deliveryDate, "Delivery date cannot be null");
        
        return deliveryDate.plusDays(getEffectiveDays());
    }
    
    /**
     * Calculate the number of days remaining in the return period.
     */
    public long getDaysRemaining(LocalDateTime deliveryDate) {
        Objects.requireNonNull(deliveryDate, "Delivery date cannot be null");
        
        LocalDateTime deadline = calculateReturnDeadline(deliveryDate);
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(deadline)) {
            return 0;
        }
        
        return ChronoUnit.DAYS.between(now, deadline);
    }
    
    /**
     * Determine if a return is about to expire (within X days of deadline).
     */
    public boolean isReturnAboutToExpire(LocalDateTime deliveryDate, int warningDays) {
        long daysRemaining = getDaysRemaining(deliveryDate);
        return daysRemaining > 0 && daysRemaining <= warningDays;
    }
    
    // Getters
    public Integer getStandardDays() { return standardDays; }
    public Integer getHolidayExtensionDays() { return holidayExtensionDays; }
    public boolean isHolidayPeriod() { return isHolidayPeriod; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainReturnPeriodValue that = (DomainReturnPeriodValue) o;
        return isHolidayPeriod == that.isHolidayPeriod &&
               Objects.equals(standardDays, that.standardDays) &&
               Objects.equals(holidayExtensionDays, that.holidayExtensionDays);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(standardDays, holidayExtensionDays, isHolidayPeriod);
    }
    
    @Override
    public String toString() {
        return String.format("DomainReturnPeriodValue{standardDays=%d, holidayExtension=%d, isHolidayPeriod=%s, effectiveDays=%d}", 
                           standardDays, holidayExtensionDays, isHolidayPeriod, getEffectiveDays());
    }
}