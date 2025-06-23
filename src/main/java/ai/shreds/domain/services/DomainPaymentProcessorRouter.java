package ai.shreds.domain.services;

import ai.shreds.domain.commands.DomainProcessorChargeResult;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;
import ai.shreds.domain.entities.DomainThreeDSecureEntity;
import ai.shreds.domain.exceptions.DomainPaymentException;
import ai.shreds.domain.ports.DomainOutputPortPaymentProcessor;
import ai.shreds.domain.value_objects.DomainPaymentProcessorTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Router service that selects the appropriate payment processor based on the payment intent.
 * Implements strategy pattern to route payments to different external processors.
 */
@Service
public class DomainPaymentProcessorRouter {
    private final Map<DomainPaymentProcessorTypeEnum, DomainOutputPortPaymentProcessor> processors;

    @Autowired
    public DomainPaymentProcessorRouter(
            @Qualifier("infrastructureStripeProcessorAdapter") DomainOutputPortPaymentProcessor stripeProcessor,
            @Qualifier("infrastructurePayPalProcessorAdapter") DomainOutputPortPaymentProcessor paypalProcessor,
            @Qualifier("infrastructureSquareProcessorAdapter") DomainOutputPortPaymentProcessor squareProcessor) {
        this.processors = new HashMap<>();
        this.processors.put(DomainPaymentProcessorTypeEnum.STRIPE, stripeProcessor);
        this.processors.put(DomainPaymentProcessorTypeEnum.PAYPAL, paypalProcessor);
        this.processors.put(DomainPaymentProcessorTypeEnum.SQUARE, squareProcessor);
        validateProcessors();
    }

    /**
     * Routes and charges a payment intent using the appropriate processor.
     * 
     * @param intent the payment intent to charge
     * @param threeDSecure optional 3D Secure entity for authentication
     * @return the charge result from the processor
     */
    public DomainProcessorChargeResult routeAndCharge(DomainPaymentIntentEntity intent, DomainThreeDSecureEntity threeDSecure) {
        Objects.requireNonNull(intent, "intent cannot be null");
        
        DomainPaymentProcessorTypeEnum processorType = intent.getProcessorType();
        DomainOutputPortPaymentProcessor processor = processors.get(processorType);
        
        if (processor == null) {
            throw DomainPaymentException.processorUnavailable(processorType.toString());
        }
        
        try {
            return processor.charge(intent, threeDSecure);
        } catch (Exception e) {
            throw new DomainPaymentException(
                "Failed to charge payment through processor: " + processorType,
                "PROCESSOR_CHARGE_FAILED",
                e
            );
        }
    }

    /**
     * Routes and charges a payment intent without 3D Secure.
     * 
     * @param intent the payment intent to charge
     * @return the charge result from the processor
     */
    public DomainProcessorChargeResult routeAndCharge(DomainPaymentIntentEntity intent) {
        return routeAndCharge(intent, null);
    }

    /**
     * Checks if a processor is available for the given processor type.
     * 
     * @param processorType the processor type to check
     * @return true if processor is available, false otherwise
     */
    public boolean isProcessorAvailable(DomainPaymentProcessorTypeEnum processorType) {
        return processors.containsKey(processorType) && processors.get(processorType) != null;
    }

    /**
     * Gets all available processor types.
     * 
     * @return set of available processor types
     */
    public Set<DomainPaymentProcessorTypeEnum> getAvailableProcessorTypes() {
        return processors.keySet();
    }

    /**
     * Selects the best processor for the given payment intent based on business rules.
     * This can be used for automatic processor selection.
     * 
     * @param intent the payment intent
     * @return the recommended processor type
     */
    public DomainPaymentProcessorTypeEnum selectOptimalProcessor(DomainPaymentIntentEntity intent) {
        Objects.requireNonNull(intent, "intent cannot be null");
        
        // If intent already has a processor type, validate it's available
        DomainPaymentProcessorTypeEnum currentType = intent.getProcessorType();
        if (isProcessorAvailable(currentType)) {
            return currentType;
        }
        
        // Fallback selection logic based on business rules
        java.math.BigDecimal amount = intent.getAmount().getAmount();
        String currency = intent.getAmount().getCurrency();
        
        // High-value payments prefer Stripe for better fraud protection
        if (amount.compareTo(java.math.BigDecimal.valueOf(1000)) > 0 && 
            isProcessorAvailable(DomainPaymentProcessorTypeEnum.STRIPE)) {
            return DomainPaymentProcessorTypeEnum.STRIPE;
        }
        
        // USD/EUR payments prefer PayPal for international support
        if (("USD".equals(currency) || "EUR".equals(currency)) && 
            isProcessorAvailable(DomainPaymentProcessorTypeEnum.PAYPAL)) {
            return DomainPaymentProcessorTypeEnum.PAYPAL;
        }
        
        // Default fallback
        if (isProcessorAvailable(DomainPaymentProcessorTypeEnum.SQUARE)) {
            return DomainPaymentProcessorTypeEnum.SQUARE;
        }
        
        // If no processors available, throw exception
        throw DomainPaymentException.processorUnavailable("No available processors");
    }

    /**
     * Gets the processor instance for a given type.
     * 
     * @param processorType the processor type
     * @return the processor instance or null if not available
     */
    public DomainOutputPortPaymentProcessor getProcessor(DomainPaymentProcessorTypeEnum processorType) {
        return processors.get(processorType);
    }

    /**
     * Checks if any processors are available.
     * 
     * @return true if at least one processor is available
     */
    public boolean hasAvailableProcessors() {
        return !processors.isEmpty();
    }

    private void validateProcessors() {
        if (processors.isEmpty()) {
            throw new IllegalArgumentException("At least one processor must be configured");
        }
        
        // Validate that all processor instances are not null
        for (Map.Entry<DomainPaymentProcessorTypeEnum, DomainOutputPortPaymentProcessor> entry : processors.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Processor instance cannot be null for type: " + entry.getKey());
            }
        }
    }

    @Override
    public String toString() {
        return "DomainPaymentProcessorRouter{" +
                "availableProcessors=" + processors.keySet() +
                '}';
    }
}