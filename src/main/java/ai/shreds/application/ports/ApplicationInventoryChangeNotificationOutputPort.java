package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedInventoryChangeMessage;

public interface ApplicationInventoryChangeNotificationOutputPort {
    /**
     * Sends an inventory change notification via JMS
     *
     * @param message The inventory change message containing change details
     */
    void sendInventoryChangeNotification(SharedInventoryChangeMessage message);
}