package ai.shreds.domain.ports;

import ai.shreds.domain.value_objects.DomainOrderAggregate;
import ai.shreds.domain.value_objects.DomainOrderIdValue;

import java.util.Optional;

/**
 * Domain output port for order persistence.
 * This port is implemented by infrastructure layer.
 */
public interface DomainOutputPortOrderRepository {
    
    /**
     * Saves an order aggregate to the persistent store.
     *
     * @param order the order aggregate to save
     * @return the saved order aggregate with generated identifiers
     */
    DomainOrderAggregate save(DomainOrderAggregate order);
    
    /**
     * Finds an order by its cart ID for idempotency checks.
     *
     * @param cartId the cart identifier
     * @return optional containing the order if found
     */
    Optional<DomainOrderAggregate> findByCartId(String cartId);
    
    /**
     * Finds an order by its unique identifier.
     *
     * @param orderId the order identifier
     * @return optional containing the order if found
     */
    Optional<DomainOrderAggregate> findById(DomainOrderIdValue orderId);
}