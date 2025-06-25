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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    @CacheEvict(value = "orderCache", key = "#orderAggregate.getOrder().getCartId()")
    public DomainOrderAggregate save(DomainOrderAggregate orderAggregate) {
        try {
            String cartId = orderAggregate.getOrder().getCartId();
            
            // Check if order already exists for this cartId (idempotency)
            Optional<DomainOrderAggregate> existingOrder = findByCartId(cartId);
            if (existingOrder.isPresent()) {
                // Return existing order instead of creating duplicate
                return existingOrder.get();
            }
            
            // Clean up any orphaned records for this order ID (in case of previous partial failures)
            UUID orderId = orderAggregate.getOrder().getOrderId();
            cleanupOrphanedRecords(orderId);
            
            // 1. Save addresses first
            DomainAddressEntity savedBilling = addressJpaRepository.save(orderAggregate.getBillingAddress());
            DomainAddressEntity savedShipping = addressJpaRepository.save(orderAggregate.getShippingAddress());
            
            // 2. Prepare and save order entity
            DomainOrderEntity orderEntity = orderMapper.fromAggregate(orderAggregate);
            orderEntity.setBillingAddressId(savedBilling.getAddressId());
            orderEntity.setShippingAddressId(savedShipping.getAddressId());
            
            DomainOrderEntity savedOrder = orderJpaRepository.save(orderEntity);
            
            // 3. Create and save order items
            for (DomainOrderItemEntity originalItem : orderAggregate.getOrderItems()) {
                DomainOrderItemEntity newItem = DomainOrderItemEntity.builder()
                    .orderItemId(UUID.randomUUID())
                    .orderId(savedOrder.getOrderId())
                    .productId(originalItem.getProductId())
                    .quantity(originalItem.getQuantity())
                    .unitPrice(originalItem.getUnitPrice())
                    .totalPrice(originalItem.getTotalPrice())
                    .currency(originalItem.getCurrency())
                    .itemStatus(originalItem.getItemStatus())
                    .createdAt(originalItem.getCreatedAt())
                    .build();
                
                orderItemJpaRepository.save(newItem);
            }
            
            // 4. Create and save payment details
            DomainPaymentDetailsEntity newPaymentDetails = DomainPaymentDetailsEntity.builder()
                .paymentId(UUID.randomUUID())
                .orderId(savedOrder.getOrderId())
                .paymentStatus(orderAggregate.getPaymentDetails().getPaymentStatus())
                .paymentAmount(orderAggregate.getPaymentDetails().getPaymentAmount())
                .currency(orderAggregate.getPaymentDetails().getCurrency())
                .paymentProvider(orderAggregate.getPaymentDetails().getPaymentProvider())
                .createdAt(orderAggregate.getPaymentDetails().getCreatedAt())
                .build();
                
            paymentDetailsJpaRepository.save(newPaymentDetails);
            
            // 5. Create and save shipping details
            DomainShippingDetailsEntity newShippingDetails = DomainShippingDetailsEntity.builder()
                .shippingId(UUID.randomUUID())
                .orderId(savedOrder.getOrderId())
                .shippingStatus(orderAggregate.getShippingDetails().getShippingStatus())
                .trackingNumber(orderAggregate.getShippingDetails().getTrackingNumber())
                .carrier(orderAggregate.getShippingDetails().getCarrier())
                .createdAt(orderAggregate.getShippingDetails().getCreatedAt())
                .build();
                
            shippingDetailsJpaRepository.save(newShippingDetails);
            
            // 6. Return the saved aggregate by reloading from database
            return findByCartId(savedOrder.getCartId()).orElseThrow(
                () -> new InfrastructurePersistenceException(
                    "Failed to reload saved order", null, 
                    "Order not found after save", 
                    DomainOrderEntity.class.getSimpleName()
                )
            );
            
        } catch (DataIntegrityViolationException ex) {
            // Handle constraint violations gracefully - might be duplicate request
            String cartId = orderAggregate.getOrder().getCartId();
            Optional<DomainOrderAggregate> existingOrder = findByCartId(cartId);
            if (existingOrder.isPresent()) {
                // Return existing order if constraint violation was due to duplicate
                return existingOrder.get();
            }
            throw new InfrastructurePersistenceException(
                "Data integrity violation during order save", ex,
                ex.getMessage(), DomainOrderEntity.class.getSimpleName());
        } catch (Exception ex) {
            throw new InfrastructurePersistenceException("Failed to persist order", ex,
                    ex.getMessage(), DomainOrderEntity.class.getSimpleName());
        }
    }
    
    private void cleanupOrphanedRecords(UUID orderId) {
        try {
            // Clean up any orphaned payment details
            paymentDetailsJpaRepository.findByOrderId(orderId)
                .ifPresent(paymentDetailsJpaRepository::delete);
                
            // Clean up any orphaned shipping details
            shippingDetailsJpaRepository.findByOrderId(orderId)
                .ifPresent(shippingDetailsJpaRepository::delete);
                
            // Clean up any orphaned order items
            List<DomainOrderItemEntity> orphanedItems = orderItemJpaRepository.findAllByOrderId(orderId);
            if (!orphanedItems.isEmpty()) {
                orderItemJpaRepository.deleteAll(orphanedItems);
            }
        } catch (Exception ex) {
            // Log but don't fail the main operation
            // In a real system, you'd use proper logging here
            System.err.println("Warning: Failed to cleanup orphaned records for orderId: " + orderId + ", error: " + ex.getMessage());
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