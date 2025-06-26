package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.shared.enums.SharedOrderStatusEnum;
import java.util.List;

/**
 * Domain input port for order state transitions.
 * This port defines the contract for validating and executing order status transitions.
 */
public interface DomainInputPortOrderStateTransition {

    /**
     * Validates if a transition from current status to new status is allowed.
     * @param currentStatus The current order status
     * @param newStatus The target order status
     * @return true if transition is valid, false otherwise
     */
    boolean validateTransition(SharedOrderStatusEnum currentStatus, SharedOrderStatusEnum newStatus);

    /**
     * Executes the order state transition and applies business rules.
     * @param order The order entity to transition
     * @param newStatus The target status
     * @return The updated order entity
     * @throws ai.shreds.domain.exceptions.DomainInvalidStateTransitionException if transition is invalid
     */
    DomainOrderEntity transitionOrder(DomainOrderEntity order, SharedOrderStatusEnum newStatus);

    /**
     * Gets the list of valid next statuses from the current status.
     * @param currentStatus The current order status
     * @return List of valid next statuses
     */
    List<SharedOrderStatusEnum> getNextValidStatuses(SharedOrderStatusEnum currentStatus);
}