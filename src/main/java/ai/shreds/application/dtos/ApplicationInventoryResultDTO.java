package ai.shreds.application.dtos;

public class ApplicationInventoryResultDTO {

    private String productId;
    private boolean available;
    private Integer availableQty;

    public ApplicationInventoryResultDTO() {
    }

    public ApplicationInventoryResultDTO(String productId, boolean available, Integer availableQty) {
        this.productId = productId;
        this.available = available;
        this.availableQty = availableQty;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Integer getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }
}
