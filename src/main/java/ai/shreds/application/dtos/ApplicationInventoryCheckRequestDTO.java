package ai.shreds.application.dtos;

import java.util.ArrayList;
import java.util.List;

public class ApplicationInventoryCheckRequestDTO {

    private List<ApplicationInventoryItemDTO> items;

    public ApplicationInventoryCheckRequestDTO() {
        this.items = new ArrayList<>();
    }

    public ApplicationInventoryCheckRequestDTO(List<ApplicationInventoryItemDTO> items) {
        this.items = items;
    }

    public List<ApplicationInventoryItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ApplicationInventoryItemDTO> items) {
        this.items = items;
    }

    public void addItem(ApplicationInventoryItemDTO item) {
        this.items.add(item);
    }
}
