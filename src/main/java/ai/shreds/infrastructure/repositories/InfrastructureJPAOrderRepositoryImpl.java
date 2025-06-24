package ai.shreds.infrastructure.repositories;

import ai.shreds.application.ports.ApplicationOrderRepositoryOutputPort;
import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.entities.DomainShippingDetailsEntity;
import ai.shreds.domain.entities.DomainAddressEntity;
import ai.shreds.domain.value_objects.DomainOrderAggregate;
import ai.shreds.domain.value_objects.DomainOrderIdValue;
import ai.shreds.domain.ports.DomainOutputPortOrderRepository;
import ai.shreds.infrastructure.exceptions.InfrastructurePersistenceException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InfrastructureJPAOrderRepositoryImpl implements DomainOutputPortOrderRepository, ApplicationOrderRepositoryOutputPort {

    private final InfrastructureOrderJpaRepository orderJpaRepository;
    private final InfrastructureOrderItemJpaRepository orderItemJpaRepository;
    private final InfrastructurePaymentDetailsJpaRepository paymentDetailsJpaRepository;
    private final InfrastructureShippingDetailsJpaRepository shippingDetailsJpaRepository;
    private final InfrastructureAddressJpaRepository addressJpaRepository;
    private final InfrastructureOrderMapper orderMapper;
    private final CacheManager cacheManager;

    public InfrastructureJPAOrderRepositoryImpl(InfrastructureOrderJpaRepository orderJpaRepository,
                                                InfrastructureOrderItemJpaRepository orderItemJpaRepository,
                                                InfrastructurePaymentDetailsJpaRepository paymentDetailsJpaRepository,
                                                InfrastructureShippingDetailsJpaRepository shippingDetailsJpaRepository,
                                                InfrastructureAddressJpaRepository addressJpaRepository,
                                                InfrastructureOrderMapper orderMapper,
                                                CacheManager cacheManager) {
        this.orderJpaRepository = orderJpaRepository;
        this.orderItemJpaRepository = orderItemJpaRepository;
        this.paymentDetailsJpaRepository = paymentDetailsJpaRepository;
        this.shippingDetailsJpaRepository = shippingDetailsJpaRepository;
        this.addressJpaRepository = addressJpaRepository;
        this.orderMapper = orderMapper;
        this.cacheManager = cacheManager;
    }

    @Override
    @CacheEvict(value = "orderCache", key = "#orderAggregate.getOrder().getCartId()")
    public DomainOrderAggregate save(DomainOrderAggregate orderAggregate) {
        try {
            // Save addresses
            DomainAddressEntity billing = orderAggregate.getBillingAddress();
            DomainAddressEntity shipping = orderAggregate.getShippingAddress();
            addressJpaRepository.save(billing);
            addressJpaRepository.save(shipping);
            // Persist order entity
            DomainOrderEntity orderEntity = orderMapper.fromAggregate(orderAggregate);
            orderEntity.setBillingAddressId(billing.getAddressId());
            orderEntity.setShippingAddressId(shipping.getAddressId());
            DomainOrderEntity savedOrder = orderJpaRepository.save(orderEntity);
            UUID orderId = savedOrder.getOrderId();
            // Persist order items
            orderItemJpaRepository.deleteAllByOrderId(orderId);
            for (DomainOrderItemEntity item : orderAggregate.getOrderItems()) {
                item.setOrderId(orderId);
                orderItemJpaRepository.save(item);
            }
            // Persist payment details
            DomainPaymentDetailsEntity paymentDetails = orderAggregate.getPaymentDetails();
            paymentDetails.setOrderId(orderId);
            paymentDetailsJpaRepository.save(paymentDetails);
            // Persist shipping details
            DomainShippingDetailsEntity shippingDetails = orderAggregate.getShippingDetails();
            shippingDetails.setOrderId(orderId);
            shippingDetailsJpaRepository.save(shippingDetails);
            // Return reloaded aggregate
            return findByCartId(savedOrder.getCartId()).orElse(orderAggregate);
        } catch (Exception ex) {
            throw new InfrastructurePersistenceException("Failed to persist order", ex,
                    ex.getMessage(), DomainOrderEntity.class.getSimpleName());
        }
    }

    @Override
    @Cacheable(value = "orderCache", key = "#cartId")
    public Optional<DomainOrderAggregate> findByCartId(String cartId) {
        Optional<DomainOrderEntity> orderOpt = orderJpaRepository.findByCartId(cartId);
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }
        DomainOrderEntity orderEntity = orderOpt.get();
        UUID orderId = orderEntity.getOrderId();
        List<DomainOrderItemEntity> items = orderItemJpaRepository.findAllByOrderId(orderId);
        Optional<DomainPaymentDetailsEntity> paymentDetails = paymentDetailsJpaRepository.findByOrderId(orderId);
        Optional<DomainShippingDetailsEntity> shippingDetails = shippingDetailsJpaRepository.findByOrderId(orderId);
        Optional<DomainAddressEntity> billingAddress = addressJpaRepository.findById(orderEntity.getBillingAddressId());
        Optional<DomainAddressEntity> shippingAddress = addressJpaRepository.findById(orderEntity.getShippingAddressId());
        DomainOrderAggregate aggregate = orderMapper.toAggregate(
                orderEntity,
                items,
                paymentDetails.orElse(null),
                shippingDetails.orElse(null),
                billingAddress.orElse(null),
                shippingAddress.orElse(null)
        );
        return Optional.of(aggregate);
    }

    @Override
    public Optional<DomainOrderAggregate> findById(DomainOrderIdValue orderIdValue) {
        Optional<DomainOrderEntity> orderOpt = orderJpaRepository.findById(orderIdValue.getValue());
        if (orderOpt.isEmpty()) {
            return Optional.empty();
        }
        DomainOrderEntity orderEntity = orderOpt.get();
        UUID orderId = orderEntity.getOrderId();
        List<DomainOrderItemEntity> items = orderItemJpaRepository.findAllByOrderId(orderId);
        Optional<DomainPaymentDetailsEntity> paymentDetails = paymentDetailsJpaRepository.findByOrderId(orderId);
        Optional<DomainShippingDetailsEntity> shippingDetails = shippingDetailsJpaRepository.findByOrderId(orderId);
        Optional<DomainAddressEntity> billingAddress = addressJpaRepository.findById(orderEntity.getBillingAddressId());
        Optional<DomainAddressEntity> shippingAddress = addressJpaRepository.findById(orderEntity.getShippingAddressId());
        DomainOrderAggregate aggregate = orderMapper.toAggregate(
                orderEntity,
                items,
                paymentDetails.orElse(null),
                shippingDetails.orElse(null),
                billingAddress.orElse(null),
                shippingAddress.orElse(null)
        );
        return Optional.of(aggregate);
    }
}