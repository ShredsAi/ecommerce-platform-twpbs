package ai.shreds.domain.services;

import ai.shreds.domain.value_objects.DomainValueWebhookData;
import ai.shreds.shared.enums.SharedEnumPaymentProcessorType;
import ai.shreds.shared.value_objects.SharedValueMoney;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Domain service for parsing webhook payloads from different payment processors into a standardized format.
 */
public class DomainServiceWebhookParserService {
    
    private final ObjectMapper objectMapper;
    
    public DomainServiceWebhookParserService() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses a webhook payload based on the processor type.
     *
     * @param rawPayload The raw JSON payload as received from the payment processor
     * @param processorType The type of payment processor that sent the webhook
     * @return A standardized webhook data object
     */
    public DomainValueWebhookData parseWebhookPayload(String rawPayload, SharedEnumPaymentProcessorType processorType) {
        try {
            switch (processorType) {
                case STRIPE:
                    return parseStripePayload(rawPayload);
                case PAYPAL:
                    return parsePayPalPayload(rawPayload);
                case SQUARE:
                    return parseSquarePayload(rawPayload);
                default:
                    throw new IllegalArgumentException("Unsupported payment processor type: " + processorType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse webhook payload for processor " + processorType, e);
        }
    }

    /**
     * Parses Stripe webhook payload
     */
    private DomainValueWebhookData parseStripePayload(String rawPayload) throws Exception {
        JsonNode json = objectMapper.readTree(rawPayload);
        JsonNode dataObject = json.path("data").path("object");
        
        String paymentIntentId = dataObject.path("id").asText();
        String customerId = dataObject.path("customer").asText(null);
        
        // Metadata often contains order information in Stripe
        JsonNode metadata = dataObject.path("metadata");
        String orderId = metadata.path("order_id").asText(null);
        
        // Convert amount from cents to dollars
        BigDecimal amountValue = new BigDecimal(dataObject.path("amount").asLong()).divide(new BigDecimal(100));
        String currency = dataObject.path("currency").asText("USD").toUpperCase();
        SharedValueMoney amount = new SharedValueMoney(amountValue, currency);
        
        String status = dataObject.path("status").asText();
        String processorTransactionId = paymentIntentId;
        
        // Parse timestamp from created field (Unix timestamp in seconds)
        long created = json.path("created").asLong();
        LocalDateTime timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneOffset.UTC);
        
        return new DomainValueWebhookData(paymentIntentId, customerId, orderId, amount, status, processorTransactionId, timestamp);
    }
    
    /**
     * Parses PayPal webhook payload
     */
    private DomainValueWebhookData parsePayPalPayload(String rawPayload) throws Exception {
        JsonNode json = objectMapper.readTree(rawPayload);
        JsonNode resource = json.path("resource");
        
        String paymentIntentId = resource.path("id").asText(null);
        String customerId = resource.path("payer").path("payer_id").asText(null);
        
        // PayPal often includes purchase_units with order info
        JsonNode purchaseUnits = resource.path("purchase_units");
        String orderId = null;
        JsonNode amountJson = null;
        
        if (purchaseUnits.isArray() && purchaseUnits.size() > 0) {
            JsonNode purchaseUnit = purchaseUnits.get(0);
            orderId = purchaseUnit.path("reference_id").asText(null);
            amountJson = purchaseUnit.path("amount");
        }
        
        // Try to get amount from resource level if not in purchase units
        if (amountJson == null || amountJson.isMissingNode()) {
            amountJson = resource.path("amount");
        }
        
        BigDecimal amountValue = BigDecimal.ZERO;
        String currency = "USD";
        
        if (amountJson != null && !amountJson.isMissingNode()) {
            amountValue = new BigDecimal(amountJson.path("value").asText("0"));
            currency = amountJson.path("currency_code").asText("USD");
        }
        
        SharedValueMoney amount = new SharedValueMoney(amountValue, currency);
        
        String status = resource.path("status").asText();
        String processorTransactionId = resource.path("id").asText(
            resource.path("transaction_id").asText(
                resource.path("payment_id").asText(null)
            )
        );
        
        // Parse timestamp
        String createdTime = json.path("create_time").asText();
        LocalDateTime timestamp = LocalDateTime.parse(createdTime.substring(0, 19));
        
        return new DomainValueWebhookData(paymentIntentId, customerId, orderId, amount, status, processorTransactionId, timestamp);
    }
    
    /**
     * Parses Square webhook payload
     */
    private DomainValueWebhookData parseSquarePayload(String rawPayload) throws Exception {
        JsonNode json = objectMapper.readTree(rawPayload);
        JsonNode data = json.path("data");
        JsonNode object = data.path("object");
        
        String paymentIntentId = object.path("id").asText();
        String customerId = object.path("customer_id").asText(null);
        String orderId = object.path("order_id").asText(null);
        
        // Parse amount information
        JsonNode amountMoney = object.path("amount_money");
        BigDecimal amountValue = new BigDecimal(amountMoney.path("amount").asLong()).divide(new BigDecimal(100));
        String currency = amountMoney.path("currency").asText("USD");
        SharedValueMoney amount = new SharedValueMoney(amountValue, currency);
        
        String status = object.path("status").asText();
        String processorTransactionId = paymentIntentId;
        
        // Parse timestamp
        String created = json.path("created_at").asText();
        LocalDateTime timestamp = LocalDateTime.parse(created.substring(0, 19));
        
        return new DomainValueWebhookData(paymentIntentId, customerId, orderId, amount, status, processorTransactionId, timestamp);
    }
}
