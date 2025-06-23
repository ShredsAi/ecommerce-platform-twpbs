package ai.shreds.domain.services;

import ai.shreds.domain.exceptions.DomainInvalidStateException;
import ai.shreds.domain.value_objects.DomainPaymentStatusEnum;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Service enforcing valid payment status transitions.
 */
@Service
public class DomainPaymentStateMachine {
    private final Map<DomainPaymentStatusEnum, Set<DomainPaymentStatusEnum>> validTransitions;

    public DomainPaymentStateMachine() {
        validTransitions = new EnumMap<>(DomainPaymentStatusEnum.class);
        initializeTransitions();
    }

    private void initializeTransitions() {
        validTransitions.put(DomainPaymentStatusEnum.REQUIRES_PAYMENT_METHOD,
                Set.of(DomainPaymentStatusEnum.REQUIRES_CONFIRMATION));
        
        validTransitions.put(DomainPaymentStatusEnum.REQUIRES_CONFIRMATION,
                Set.of(DomainPaymentStatusEnum.PROCESSING, DomainPaymentStatusEnum.FAILED));
        
        validTransitions.put(DomainPaymentStatusEnum.PROCESSING,
                Set.of(DomainPaymentStatusEnum.SUCCEEDED, DomainPaymentStatusEnum.FAILED));
        
        validTransitions.put(DomainPaymentStatusEnum.SUCCEEDED, Set.of());
        validTransitions.put(DomainPaymentStatusEnum.FAILED, Set.of());
    }

    /**
     * Validates if a status transition is allowed.
     * @param current Current payment status
     * @param next Desired next status
     * @throws DomainInvalidStateException if transition is not allowed
     */
    public void validateAndTransition(DomainPaymentStatusEnum current, DomainPaymentStatusEnum next) {
        if (!isValidTransition(current, next)) {
            throw new DomainInvalidStateException(current, next);
        }
    }

    private boolean isValidTransition(DomainPaymentStatusEnum current, DomainPaymentStatusEnum next) {
        return validTransitions.containsKey(current) && validTransitions.get(current).contains(next);
    }
}