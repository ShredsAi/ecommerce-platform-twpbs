package ai.shreds.infrastructure.repositories;

import ai.shreds.domain.entities.DomainPaymentEntity;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainPaymentStatusUpdateEntity;
import ai.shreds.domain.ports.DomainOutputPortPaymentRepository;
import ai.shreds.domain.value_objects.DomainPaymentIdValue;
import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import ai.shreds.infrastructure.exceptions.InfrastructureRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class InfrastructurePaymentRepositoryImpl implements DomainOutputPortPaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(InfrastructurePaymentRepositoryImpl.class);

    private final InfrastructurePaymentIntentJpaRepository paymentIntentRepository;
    private final InfrastructurePaymentJpaRepository paymentRepository;
    private final InfrastructurePaymentMethodJpaRepository paymentMethodRepository;
    private final InfrastructurePaymentTokenJpaRepository paymentTokenRepository;
    private final InfrastructureThreeDSecureJpaRepository threeDSecureRepository;
    private final InfrastructurePaymentStatusUpdateJpaRepository statusUpdateRepository;
    private final InfrastructurePaymentWebhookCorrelationJpaRepository webhookCorrelationRepository;
    private final InfrastructureJpaEntityMapper mapper;

    public InfrastructurePaymentRepositoryImpl(
            InfrastructurePaymentIntentJpaRepository paymentIntentRepository,
            InfrastructurePaymentJpaRepository paymentRepository,
            InfrastructurePaymentMethodJpaRepository paymentMethodRepository,
            InfrastructurePaymentTokenJpaRepository paymentTokenRepository,
            InfrastructureThreeDSecureJpaRepository threeDSecureRepository,
            InfrastructurePaymentStatusUpdateJpaRepository statusUpdateRepository,
            InfrastructurePaymentWebhookCorrelationJpaRepository webhookCorrelationRepository,
            InfrastructureJpaEntityMapper mapper) {
        this.paymentIntentRepository = paymentIntentRepository;
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentTokenRepository = paymentTokenRepository;
        this.threeDSecureRepository = threeDSecureRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.webhookCorrelationRepository = webhookCorrelationRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public DomainPaymentIntentEntity savePaymentIntent(DomainPaymentIntentEntity intent) {
        try {
            log.debug("Saving payment intent with ID: {}", intent.getId().getValue());
            
            InfrastructurePaymentIntentJpaEntity jpaEntity = mapper.toPaymentIntentJpa(intent);
            InfrastructurePaymentIntentJpaEntity savedEntity = paymentIntentRepository.save(jpaEntity);
            
            DomainPaymentIntentEntity result = mapper.toPaymentIntentDomain(savedEntity);
            log.debug("Successfully saved payment intent with ID: {}", result.getId().getValue());
            
            return result;
        } catch (OptimisticLockingFailureException ex) {
            throw InfrastructureRepositoryException.optimisticLockingFailure(
                "PaymentIntent", 
                intent.getId().getValue().toString(), 
                ex
            );
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentIntent", 
                "save", 
                ex
            );
        } catch (Exception ex) {
            log.error("Unexpected error saving payment intent: {}", ex.getMessage(), ex);
            throw new InfrastructureRepositoryException(
                "PaymentIntent", 
                "save", 
                "Unexpected error saving payment intent: " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public DomainPaymentIntentEntity findPaymentIntentById(DomainPaymentIntentIdValue id) {
        try {
            log.debug("Finding payment intent with ID: {}", id.getValue());
            
            return paymentIntentRepository.findById(id.getValue())
                .map(mapper::toPaymentIntentDomain)
                .orElse(null); // Return null instead of throwing exception for consistent behavior
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentIntent", 
                "find", 
                ex
            );
        }
    }

    @Override
    @Transactional
    public DomainPaymentEntity savePayment(DomainPaymentEntity payment) {
        try {
            log.debug("Saving payment with ID: {}", payment.getId().getValue());
            
            InfrastructurePaymentJpaEntity jpaEntity = mapper.toPaymentJpa(payment);
            InfrastructurePaymentJpaEntity savedEntity = paymentRepository.save(jpaEntity);
            
            DomainPaymentEntity result = mapper.toPaymentDomain(savedEntity);
            log.debug("Successfully saved payment with ID: {}", result.getId().getValue());
            
            return result;
        } catch (OptimisticLockingFailureException ex) {
            throw InfrastructureRepositoryException.optimisticLockingFailure(
                "Payment", 
                payment.getId().getValue().toString(), 
                ex
            );
        } catch (DataAccessException ex) {
            // Check if it's a duplicate key constraint violation
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique")) {
                throw InfrastructureRepositoryException.duplicateEntity(
                    "Payment", 
                    payment.getId().getValue().toString()
                );
            }
            throw InfrastructureRepositoryException.databaseConnectivity(
                "Payment", 
                "save", 
                ex
            );
        } catch (Exception ex) {
            log.error("Unexpected error saving payment: {}", ex.getMessage(), ex);
            throw new InfrastructureRepositoryException(
                "Payment", 
                "save", 
                "Unexpected error saving payment: " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public DomainPaymentEntity findPaymentById(DomainPaymentIdValue id) {
        try {
            log.debug("Finding payment with ID: {}", id.getValue());
            
            return paymentRepository.findById(id.getValue())
                .map(mapper::toPaymentDomain)
                .orElse(null); // Return null instead of throwing exception for consistent behavior
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "Payment", 
                "find", 
                ex
            );
        }
    }

    @Override
    public List<DomainPaymentIntentEntity> findExpiredIntents() {
        try {
            log.debug("Finding expired payment intents before: {}", LocalDateTime.now());
            
            List<InfrastructurePaymentIntentJpaEntity> expiredEntities = 
                paymentIntentRepository.findByExpiresAtBefore(LocalDateTime.now());
            
            List<DomainPaymentIntentEntity> result = expiredEntities.stream()
                .map(mapper::toPaymentIntentDomain)
                .collect(Collectors.toList());
            
            log.debug("Found {} expired payment intents", result.size());
            return result;
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentIntent", 
                "findExpired", 
                ex
            );
        }
    }

    @Override
    @Transactional
    public void saveStatusUpdate(DomainPaymentStatusUpdateEntity update) {
        try {
            log.debug("Saving payment status update for payment ID: {}", 
                update.getPaymentId() != null ? update.getPaymentId().getValue() : "null");
            
            InfrastructurePaymentStatusUpdateJpaEntity jpaEntity = 
                InfrastructurePaymentStatusUpdateJpaEntity.fromDomainEntity(update);
            statusUpdateRepository.save(jpaEntity);
            
            log.debug("Successfully saved payment status update");
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentStatusUpdate", 
                "save", 
                ex
            );
        } catch (Exception ex) {
            log.error("Unexpected error saving status update: {}", ex.getMessage(), ex);
            throw new InfrastructureRepositoryException(
                "PaymentStatusUpdate", 
                "save", 
                "Unexpected error saving status update: " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public DomainPaymentEntity findPaymentByIntentId(DomainPaymentIntentIdValue intentId) {
        try {
            log.debug("Finding payment by payment intent ID: {}", intentId.getValue());
            
            return paymentRepository.findByPaymentIntentId(intentId.getValue())
                .map(mapper::toPaymentDomain)
                .orElse(null); // Return null instead of throwing exception for consistent behavior
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "Payment", 
                "findByIntentId", 
                ex
            );
        }
    }

    @Override
    public boolean existsPaymentIntentById(DomainPaymentIntentIdValue id) {
        try {
            log.debug("Checking if payment intent exists with ID: {}", id.getValue());
            
            boolean exists = paymentIntentRepository.existsById(id.getValue());
            log.debug("Payment intent with ID {} exists: {}", id.getValue(), exists);
            
            return exists;
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentIntent", 
                "exists", 
                ex
            );
        }
    }

    @Override
    public boolean existsPaymentById(DomainPaymentIdValue id) {
        try {
            log.debug("Checking if payment exists with ID: {}", id.getValue());
            
            boolean exists = paymentRepository.existsById(id.getValue());
            log.debug("Payment with ID {} exists: {}", id.getValue(), exists);
            
            return exists;
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "Payment", 
                "exists", 
                ex
            );
        }
    }

    @Override
    public List<DomainPaymentStatusUpdateEntity> findStatusUpdatesByPaymentId(DomainPaymentIdValue paymentId) {
        try {
            log.debug("Finding status updates for payment ID: {}", paymentId.getValue());
            
            List<InfrastructurePaymentStatusUpdateJpaEntity> statusUpdates = 
                statusUpdateRepository.findByPaymentIdOrderByUpdatedAtDesc(paymentId.getValue());
            
            List<DomainPaymentStatusUpdateEntity> result = statusUpdates.stream()
                .map(InfrastructurePaymentStatusUpdateJpaEntity::toDomainEntity)
                .collect(Collectors.toList());
            
            log.debug("Found {} status updates for payment ID: {}", result.size(), paymentId.getValue());
            return result;
        } catch (DataAccessException ex) {
            throw InfrastructureRepositoryException.databaseConnectivity(
                "PaymentStatusUpdate", 
                "findByPaymentId", 
                ex
            );
        }
    }
}