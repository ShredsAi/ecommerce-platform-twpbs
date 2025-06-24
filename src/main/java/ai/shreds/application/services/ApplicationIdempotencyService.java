package ai.shreds.application.services;

import ai.shreds.application.ports.ApplicationOrderRepositoryOutputPort;
import org.springframework.stereotype.Service;

@Service
public class ApplicationIdempotencyService {

    private final ApplicationOrderRepositoryOutputPort orderRepositoryPort;

    public ApplicationIdempotencyService(ApplicationOrderRepositoryOutputPort orderRepositoryPort) {
        this.orderRepositoryPort = orderRepositoryPort;
    }

    public boolean isDuplicate(String cartId) {
        return orderRepositoryPort.findByCartId(cartId).isPresent();
    }

    public void registerProcessing(String cartId) {
        // No-op: uniqueness constraint on orders.cart_id ensures idempotency
    }
}
