package ai.shreds.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for payment methods.
 */
@Repository
public interface InfrastructurePaymentMethodJpaRepository extends JpaRepository<InfrastructurePaymentMethodJpaEntity, UUID> {
    
    /**
     * Find all payment methods for a customer.
     *
     * @param customerId the customer ID
     * @return list of payment methods for the customer
     */
    List<InfrastructurePaymentMethodJpaEntity> findByCustomerId(UUID customerId);
    
    /**
     * Find all active payment methods for a customer.
     *
     * @param customerId the customer ID
     * @param isActive whether the payment method is active
     * @return list of active payment methods for the customer
     */
    List<InfrastructurePaymentMethodJpaEntity> findByCustomerIdAndIsActive(UUID customerId, boolean isActive);
    
    /**
     * Find the default payment method for a customer.
     *
     * @param customerId the customer ID
     * @param isDefault whether the payment method is default
     * @return the default payment method, if exists
     */
    Optional<InfrastructurePaymentMethodJpaEntity> findByCustomerIdAndIsDefault(UUID customerId, boolean isDefault);
}