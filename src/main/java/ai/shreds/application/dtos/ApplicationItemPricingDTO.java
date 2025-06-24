package ai.shreds.application.dtos;

import java.math.BigDecimal;

public class ApplicationItemPricingDTO {

    private String productId;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;

    public ApplicationItemPricingDTO() {
    }

    public ApplicationItemPricingDTO(String productId, BigDecimal unitPrice, BigDecimal totalPrice) {
        this.productId = productId;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
