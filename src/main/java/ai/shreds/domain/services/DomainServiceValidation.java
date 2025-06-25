package ai.shreds.domain.services;

import ai.shreds.domain.entities.DomainOrderItemEntity;
import ai.shreds.domain.exceptions.DomainValidationException;
import ai.shreds.domain.value_objects.DomainAddressValue;
import ai.shreds.domain.value_objects.DomainMoneyValue;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Domain service for validating business rules and constraints.
 */
@Service
public class DomainServiceValidation {
    
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("100000.00");
    private static final int MAX_ITEMS_PER_ORDER = 50;
    private static final int MAX_QUANTITY_PER_ITEM = 1000;

    /**
     * Validates customer ID format and constraints.
     *
     * @param customerId the customer ID to validate
     * @throws DomainValidationException if validation fails
     */
    public void validateCustomer(String customerId) {
        List<String> errors = new ArrayList<>();
        
        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer ID cannot be null or empty");
        } else {
            if (customerId.length() > 50) {
                errors.add("Customer ID cannot exceed 50 characters");
            }
            if (!CUSTOMER_ID_PATTERN.matcher(customerId).matches()) {
                errors.add("Customer ID must contain only alphanumeric characters, underscores and hyphens");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException("Customer validation failed", errors);
        }
    }

    /**
     * Validates billing and shipping addresses.
     *
     * @param billing the billing address
     * @param shipping the shipping address
     * @throws DomainValidationException if validation fails
     */
    public void validateAddresses(DomainAddressValue billing, DomainAddressValue shipping) {
        List<String> errors = new ArrayList<>();
        
        if (billing == null) {
            errors.add("Billing address cannot be null");
        } else {
            validateSingleAddress(billing, "Billing address", errors);
        }
        
        if (shipping == null) {
            errors.add("Shipping address cannot be null");
        } else {
            validateSingleAddress(shipping, "Shipping address", errors);
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException("Address validation failed", errors);
        }
    }

    /**
     * Validates cross-field business rules for the order.
     *
     * @param itemCount the number of items in the order
     * @param totalAmount the total monetary amount of the order
     * @throws DomainValidationException if validation fails
     */
    public void validateBusinessRules(Integer itemCount, DomainMoneyValue totalAmount) {
        List<String> errors = new ArrayList<>();
        
        if (itemCount == null || itemCount <= 0) {
            errors.add("Order must contain at least one item");
        } else if (itemCount > MAX_ITEMS_PER_ORDER) {
            errors.add("Order cannot contain more than " + MAX_ITEMS_PER_ORDER + " items");
        }
        
        if (totalAmount == null) {
            errors.add("Order total amount cannot be null");
        } else {
            if (totalAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Order total amount must be greater than zero");
            }
            if (totalAmount.getAmount().compareTo(MAX_ORDER_AMOUNT) > 0) {
                errors.add("Order total amount cannot exceed " + MAX_ORDER_AMOUNT + " " + totalAmount.getCurrency());
            }
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException("Business rules validation failed", errors);
        }
    }

    /**
     * Validates order items for business constraints.
     *
     * @param items the list of order items to validate
     * @throws DomainValidationException if validation fails
     */
    public void validateOrderItems(List<DomainOrderItemEntity> items) {
        List<String> errors = new ArrayList<>();
        
        if (items == null || items.isEmpty()) {
            errors.add("Order must contain at least one item");
            throw new DomainValidationException("Order items validation failed", errors);
        }
        
        if (items.size() > MAX_ITEMS_PER_ORDER) {
            errors.add("Order cannot contain more than " + MAX_ITEMS_PER_ORDER + " items");
        }
        
        for (int i = 0; i < items.size(); i++) {
            DomainOrderItemEntity item = items.get(i);
            String itemPrefix = "Item " + (i + 1) + ": ";
            
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                errors.add(itemPrefix + "Product ID cannot be null or empty");
            }
            
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                errors.add(itemPrefix + "Quantity must be greater than zero");
            } else if (item.getQuantity() > MAX_QUANTITY_PER_ITEM) {
                errors.add(itemPrefix + "Quantity cannot exceed " + MAX_QUANTITY_PER_ITEM);
            }
            
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                errors.add(itemPrefix + "Unit price must be non-negative");
            }
            
            if (item.getTotalPrice() == null || item.getTotalPrice().compareTo(BigDecimal.ZERO) < 0) {
                errors.add(itemPrefix + "Total price must be non-negative");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new DomainValidationException("Order items validation failed", errors);
        }
    }

    private void validateSingleAddress(DomainAddressValue address, String addressType, List<String> errors) {
        if (address.getStreet1() == null || address.getStreet1().trim().isEmpty()) {
            errors.add(addressType + " street1 cannot be null or empty");
        }
        
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            errors.add(addressType + " city cannot be null or empty");
        }
        
        if (address.getPostalCode() == null || address.getPostalCode().trim().isEmpty()) {
            errors.add(addressType + " postal code cannot be null or empty");
        }
        
        if (address.getCountry() == null || address.getCountry().trim().isEmpty()) {
            errors.add(addressType + " country cannot be null or empty");
        } else if (!COUNTRY_CODE_PATTERN.matcher(address.getCountry()).matches()) {
            errors.add(addressType + " country must be a valid 2-character ISO code");
        }
    }
}