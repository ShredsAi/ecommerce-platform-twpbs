package ai.shreds.application.dtos;

import ai.shreds.shared.dtos.SharedMoneyDTO;
import java.util.List;

public class ApplicationPricingResponseDTO {

    private SharedMoneyDTO subtotal;
    private SharedMoneyDTO tax;
    private SharedMoneyDTO discounts;
    private SharedMoneyDTO total;
    private List<ApplicationItemPricingDTO> itemBreakdown;

    public ApplicationPricingResponseDTO() {
    }

    public ApplicationPricingResponseDTO(SharedMoneyDTO subtotal,
                                          SharedMoneyDTO tax,
                                          SharedMoneyDTO discounts,
                                          SharedMoneyDTO total,
                                          List<ApplicationItemPricingDTO> itemBreakdown) {
        this.subtotal = subtotal;
        this.tax = tax;
        this.discounts = discounts;
        this.total = total;
        this.itemBreakdown = itemBreakdown;
    }

    public SharedMoneyDTO getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(SharedMoneyDTO subtotal) {
        this.subtotal = subtotal;
    }

    public SharedMoneyDTO getTax() {
        return tax;
    }

    public void setTax(SharedMoneyDTO tax) {
        this.tax = tax;
    }

    public SharedMoneyDTO getDiscounts() {
        return discounts;
    }

    public void setDiscounts(SharedMoneyDTO discounts) {
        this.discounts = discounts;
    }

    public SharedMoneyDTO getTotal() {
        return total;
    }

    public void setTotal(SharedMoneyDTO total) {
        this.total = total;
    }

    public List<ApplicationItemPricingDTO> getItemBreakdown() {
        return itemBreakdown;
    }

    public void setItemBreakdown(List<ApplicationItemPricingDTO> itemBreakdown) {
        this.itemBreakdown = itemBreakdown;
    }
}
