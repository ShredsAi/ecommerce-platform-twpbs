package ai.shreds.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class InfrastructureAsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureAsyncConfiguration.class);

    @Value("${payment.async.core-pool-size:5}")
    private int corePoolSize;

    @Value("${payment.async.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${payment.async.queue-capacity:25}")
    private int queueCapacity;

    @Value("${payment.async.thread-name-prefix:payment-async-}")
    private String threadNamePrefix;

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // Configure rejection policy to handle excess tasks
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // Add metrics for thread pool monitoring
        executor.setTaskDecorator(runnable -> {
            long startTime = System.currentTimeMillis();
            return () -> {
                try {
                    runnable.run();
                } finally {
                    long executionTime = System.currentTimeMillis() - startTime;
                    log.debug("Async task executed in {} ms", executionTime);
                    // In production, add metrics registry here
                }
            };
        });
        
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("Uncaught async exception in method {}: {}", method.getName(), ex.getMessage(), ex);
            // In production, this should integrate with error monitoring and alerting
            
            // For critical payment operations, you may want to implement a dead-letter queue
            // or retry mechanism here for failed async tasks
        };
    }
}