package ai.shreds.adapter.primary;

import ai.shreds.application.ports.ApplicationSafetyStockMonitorInputPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Scheduler that periodically checks safety stock rules and generates alerts.
 * Implements health monitoring and circuit breaker pattern for resilience.
 */
@Component
@RequiredArgsConstructor
public class AdapterSafetyStockMonitorScheduler implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(AdapterSafetyStockMonitorScheduler.class);

    private final ApplicationSafetyStockMonitorInputPort applicationMonitorPort;
    private final MeterRegistry meterRegistry;
    
    @Value("${app.scheduler.safetyStock.timeout:300000}") // 5 minutes timeout
    private long timeoutMillis;
    
    @Value("${app.scheduler.safetyStock.failureThreshold:5}")
    private int failureThreshold;
    
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastSuccessTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastExecutionTime = new AtomicLong(0);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "safety-stock-monitor");
        t.setDaemon(true);
        return t;
    });

    /**
     * Runs every 5 minutes to evaluate safety stock rules.
     * Uses circuit breaker pattern to prevent cascading failures.
     */
    @Scheduled(cron = "${app.scheduler.safetyStock.cron:0 0/5 * * * *}")
    public void checkSafetyStockRules() {
        if (isCircuitOpen()) {
            log.warn("[SAFETY-MONITOR] Circuit breaker is open, skipping safety stock evaluation");
            meterRegistry.counter("safety.stock.monitor.circuit.open").increment();
            return;
        }
        
        Instant startTime = Instant.now();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        String executionId = java.util.UUID.randomUUID().toString();
        
        log.info("[SAFETY-MONITOR] Starting safety stock rules evaluation (Execution: {})", executionId);
        
        try {
            // Execute with timeout to prevent hanging
            executor.submit(() -> {
                try {
                    applicationMonitorPort.checkAndGenerateAlerts();
                    onSuccess(startTime, executionId);
                } catch (Exception e) {
                    onFailure(startTime, executionId, e);
                }
            }).get(timeoutMillis, TimeUnit.MILLISECONDS);
            
        } catch (Exception e) {
            onFailure(startTime, executionId, e);
        } finally {
            lastExecutionTime.set(System.currentTimeMillis());
            
            // Record execution time
            timerSample.stop(Timer.builder("safety.stock.monitor.execution.time")
                    .description("Time to execute safety stock monitoring")
                    .register(meterRegistry));
        }
    }
    
    /**
     * Handles successful execution of safety stock monitoring.
     */
    private void onSuccess(Instant startTime, String executionId) {
        Duration executionTime = Duration.between(startTime, Instant.now());
        
        // Reset circuit breaker
        consecutiveFailures.set(0);
        lastSuccessTime.set(System.currentTimeMillis());
        
        // Record success metrics
        meterRegistry.counter("safety.stock.monitor.executions", "status", "success").increment();
        meterRegistry.gauge("safety.stock.monitor.last.success.time", lastSuccessTime.get());
        
        log.info("[SAFETY-MONITOR] Completed safety stock rules evaluation successfully in {}ms (Execution: {})",
                executionTime.toMillis(), executionId);
    }
    
    /**
     * Handles failed execution of safety stock monitoring.
     */
    private void onFailure(Instant startTime, String executionId, Exception e) {
        Duration executionTime = Duration.between(startTime, Instant.now());
        int currentFailures = consecutiveFailures.incrementAndGet();
        
        // Record failure metrics
        meterRegistry.counter("safety.stock.monitor.executions",
                "status", "failure",
                "error_class", e.getClass().getSimpleName()).increment();
        meterRegistry.gauge("safety.stock.monitor.consecutive.failures", currentFailures);
        
        log.error("[SAFETY-MONITOR] Error during safety stock rules evaluation after {}ms (Execution: {}, Failures: {})",
                executionTime.toMillis(), executionId, currentFailures, e);
        
        // Check if circuit should be opened
        if (currentFailures >= failureThreshold) {
            log.error("[SAFETY-MONITOR] Circuit breaker opened after {} consecutive failures", currentFailures);
            meterRegistry.counter("safety.stock.monitor.circuit.opened").increment();
        }
    }
    
    /**
     * Checks if the circuit breaker is open.
     * Circuit opens after consecutive failures and stays open for a cooldown period.
     */
    private boolean isCircuitOpen() {
        if (consecutiveFailures.get() < failureThreshold) {
            return false;
        }
        
        // Check if cooldown period has passed (10 minutes)
        long cooldownPeriod = 10 * 60 * 1000; // 10 minutes
        long timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessTime.get();
        
        if (timeSinceLastSuccess > cooldownPeriod) {
            log.info("[SAFETY-MONITOR] Circuit breaker cooldown period expired, attempting to close circuit");
            consecutiveFailures.set(failureThreshold - 1); // Allow one attempt
            return false;
        }
        
        return true;
    }
    
    /**
     * Provides health check information for the safety stock monitor.
     */
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        if (isCircuitOpen()) {
            builder.down()
                    .withDetail("status", "Circuit breaker is open")
                    .withDetail("consecutive_failures", consecutiveFailures.get())
                    .withDetail("last_success_time", Instant.ofEpochMilli(lastSuccessTime.get()));
        } else {
            builder.up()
                    .withDetail("status", "Operational")
                    .withDetail("consecutive_failures", consecutiveFailures.get())
                    .withDetail("last_success_time", Instant.ofEpochMilli(lastSuccessTime.get()))
                    .withDetail("last_execution_time", Instant.ofEpochMilli(lastExecutionTime.get()));
        }
        
        return builder.build();
    }
    
    /**
     * Manual trigger for safety stock monitoring (for testing or admin purposes).
     */
    public void triggerManualCheck() {
        log.info("[SAFETY-MONITOR] Manual safety stock check triggered");
        checkSafetyStockRules();
    }
    
    /**
     * Resets the circuit breaker (for admin purposes).
     */
    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        lastSuccessTime.set(System.currentTimeMillis());
        meterRegistry.counter("safety.stock.monitor.circuit.reset").increment();
        log.info("[SAFETY-MONITOR] Circuit breaker has been manually reset");
    }
}