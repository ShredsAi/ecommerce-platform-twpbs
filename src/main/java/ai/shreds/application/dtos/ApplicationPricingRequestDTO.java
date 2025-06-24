package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedAddressDTO;
import java.util.List;

public class ApplicationPricingRequestDTO {

    private String customerId;
    private List<ApplicationPricingItemDTO> items;
    private SharedAddressDTO billingAddress;
    private SharedAddressDTO shippingAddress;
    private List<String> promotions;

    public ApplicationPricingRequestDTO() {
    }

    public ApplicationPricingRequestDTO(String customerId,
                                       List<ApplicationPricingItemDTO> items,
                                       SharedAddressDTO billingAddress,
                                       SharedAddressDTO shippingAddress,
                                       List<String> promotions) {
        this.customerId = customerId;
        this.items = items;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.promotions = promotions;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<ApplicationPricingItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ApplicationPricingItemDTO> items) {
        this.items = items;
    }

    public SharedAddressDTO getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(SharedAddressDTO billingAddress) {
        this.billingAddress = billingAddress;
    }

    public SharedAddressDTO getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(SharedAddressDTO shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<String> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<String> promotions) {
        this.promotions = promotions;
    }
}
