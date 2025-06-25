package ai.shreds.application.ports;

import ai.shreds.shared.dtos.SharedRefundRequestDTO;

/**
 * Output port for payment service interactions.
 */
public interface ApplicationPaymentServiceOutputPort {

    /**
     * Initiate a refund request.
     * @param request refund request DTO
     * @return processed refund request DTO
     */
    SharedRefundRequestDTO initiateRefund(SharedRefundRequestDTO request);

    /**
     * Check the status of an existing refund.
     * @param refundId identifier of the refund
     * @return refund request DTO with updated status
     */
    SharedRefundRequestDTO checkRefundStatus(String refundId);

    /**
     * Cancel a pending refund.
     * @param refundId identifier of the refund to cancel
     */
    void cancelRefund(String refundId);
}
