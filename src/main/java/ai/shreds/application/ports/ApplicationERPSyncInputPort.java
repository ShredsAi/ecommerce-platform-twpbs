package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedERPUpdateMessage;

public interface ApplicationERPSyncInputPort {
    /**
     * Processes a batch of stock updates from ERP system
     *
     * @param message The ERP update message containing batch ID and list of adjustments
     */
    void processERPBatch(SharedERPUpdateMessage message);
}