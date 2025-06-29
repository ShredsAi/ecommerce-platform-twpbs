package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedInventoryChangedEvent;

public interface ApplicationInventoryEventOutputPort {
    /**
     * Publishes an inventory change event to notify downstream consumers
     *
     * @param event The inventory changed event containing details of the change
     */
    void publishInventoryChange(SharedInventoryChangedEvent event);
}