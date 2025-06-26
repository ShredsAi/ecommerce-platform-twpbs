package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainPaymentDetailsEntity;
import ai.shreds.domain.exceptions.DomainValidationException;
import ai.shreds.shared.value_objects.SharedAddressValue;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class DomainOrderValidationService {

    public void validateOrderConsistency(DomainOrderEntity order) {
        List<String> violations = new ArrayList<>();
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            violations.add("An order must contain at least one item.");
        }
        order.getOrderItems().forEach(item -> {
            if (item.getQuantity().getValue() <= 0) {
                violations.add("Order item '" + item.getOrderItemId() + "' has non-positive quantity.");
            }
        });
        String currency = order.getSubtotalAmount().getCurrency();
        if (!order.getTotalAmount().getCurrency().equals(currency)) {
            violations.add("Order total currency must match subtotal currency.");
        }
        if (!violations.isEmpty()) {
            throw new DomainValidationException(violations);
        }
    }

    public void validatePaymentAmount(DomainOrderEntity order, DomainPaymentDetailsEntity payment) {
        if (payment.getAmount().getValue().compareTo(order.getTotalAmount().getValue()) > 0) {
            throw new DomainValidationException(
                List.of("Payment amount " + payment.getAmount().getValue() + " exceeds order total " + order.getTotalAmount().getValue())
            );
        }
    }

    public boolean validateCancellationAllowed(DomainOrderEntity order) {
        SharedOrderStatusEnum status = order.getOrderStatus();
        switch (status) {
            case PENDING, CONFIRMED, PAID, PROCESSING:
                return true;
            default:
                return false;
        }
    }

    public void validateAddresses(SharedAddressValue billing, SharedAddressValue shipping) {
        List<String> violations = new ArrayList<>();
        if (billing == null) {
            violations.add("Billing address is required.");
        } else {
            if (billing.getStreet1() == null || billing.getStreet1().isBlank()) violations.add("Billing street1 is required.");
            if (billing.getCity() == null || billing.getCity().isBlank()) violations.add("Billing city is required.");
            if (billing.getPostalCode() == null || billing.getPostalCode().isBlank()) violations.add("Billing postal code is required.");
            if (billing.getCountry() == null || billing.getCountry().isBlank()) violations.add("Billing country is required.");
        }
        if (shipping == null) {
            violations.add("Shipping address is required.");
        } else {
            if (shipping.getStreet1() == null || shipping.getStreet1().isBlank()) violations.add("Shipping street1 is required.");
            if (shipping.getCity() == null || shipping.getCity().isBlank()) violations.add("Shipping city is required.");
            if (shipping.getPostalCode() == null || shipping.getPostalCode().isBlank()) violations.add("Shipping postal code is required.");
            if (shipping.getCountry() == null || shipping.getCountry().isBlank()) violations.add("Shipping country is required.");
        }
        if (!violations.isEmpty()) {
            throw new DomainValidationException(violations);
        }
    }
}