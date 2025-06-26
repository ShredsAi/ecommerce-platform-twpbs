package ai.shreds.infrastructure.external_services;

import ai.shreds.adapters.primary.AdapterShippingUpdateStreamObserver;
import ai.shreds.shared.dtos.SharedShippingUpdateDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class InfrastructureShippingStreamObserver {

    private final AdapterShippingUpdateStreamObserver adapterStreamObserver;
    private final ScheduledExecutorService executorService;
    private boolean streaming = false;

    public InfrastructureShippingStreamObserver(AdapterShippingUpdateStreamObserver adapterStreamObserver,
                                                ScheduledExecutorService executorService) {
        this.adapterStreamObserver = adapterStreamObserver;
        this.executorService = executorService;
    }

    public void onNext(SharedShippingUpdateDTO update) {
        try {
            adapterStreamObserver.onNext(update);
        } catch (Exception e) {
            onError(e);
        }
    }

    public void onError(Throwable throwable) {
        streaming = false;
        adapterStreamObserver.onError(throwable);
        // Implement reconnection strategy
        executorService.schedule(this::reconnect, 5, TimeUnit.SECONDS);
    }

    public void onCompleted() {
        streaming = false;
        adapterStreamObserver.onCompleted();
    }

    public void startStreaming(UUID orderId) {
        if (!streaming) {
            streaming = true;
            // In a real implementation, this would connect to the gRPC stream
            // and forward events to the adapter
        }
    }

    public void reconnect() {
        // Implement reconnection logic to the gRPC service
        if (!streaming) {
            streaming = true;
            // Reconnect to the stream
        }
    }
}