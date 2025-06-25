package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedAddressDTO;
import ai.shreds.shared.dtos.SharedPaymentMethodDTO;
import ai.shreds.shared.dtos.SharedCartCheckedOutEventDTO;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationOrderCreationRequestDTO {

    private String customerId;
    private String cartId;
    private List<ApplicationInventoryItemDTO> items;
    private SharedAddressDTO billingAddress;
    private SharedAddressDTO shippingAddress;
    private SharedPaymentMethodDTO paymentMethod;

    public ApplicationOrderCreationRequestDTO() {
    }

    public ApplicationOrderCreationRequestDTO(String customerId,
                                              String cartId,
                                              List<ApplicationInventoryItemDTO> items,
                                              SharedAddressDTO billingAddress,
                                              SharedAddressDTO shippingAddress,
                                              SharedPaymentMethodDTO paymentMethod) {
        this.customerId = customerId;
        this.cartId = cartId;
        this.items = items;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public List<ApplicationInventoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ApplicationInventoryItemDTO> items) {
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

    public SharedPaymentMethodDTO getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(SharedPaymentMethodDTO paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public static ApplicationOrderCreationRequestDTO fromSharedDTO(SharedCartCheckedOutEventDTO dto) {
        ApplicationOrderCreationRequestDTO request = new ApplicationOrderCreationRequestDTO();
        request.setCustomerId(dto.getCustomerId());
        request.setCartId(dto.getCartId());
        request.setItems(
            dto.getItems()
               .stream()
               .map(i -> new ApplicationInventoryItemDTO(i.getProductId(), i.getQuantity()))
               .collect(Collectors.toList())
        );
        request.setBillingAddress(dto.getBillingAddress());
        request.setShippingAddress(dto.getShippingAddress());
        request.setPaymentMethod(dto.getPaymentMethod());
        return request;
    }
}
