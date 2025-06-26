package ai.shreds.domain.ports;

import ai.shreds.domain.entities.DomainOrderEntity;
import ai.shreds.domain.entities.DomainSagaStateEntity;
import ai.shreds.shared.enums.SharedSagaStepEnum;

/**
 * Domain input port for saga coordination.
 * This port defines the contract for saga orchestration operations.
 */
public interface DomainInputPortSagaCoordination {

    /**
     * Initiates a new saga for the given order.
     * @param order The order entity to start saga for
     * @return The created saga state entity
     * @throws ai.shreds.domain.exceptions.DomainSagaException if saga initiation fails
     */
    DomainSagaStateEntity initiateSaga(DomainOrderEntity order);

    /**
     * Processes a specific step in the saga workflow.
     * @param sagaState The current saga state
     * @param step The step to process
     * @throws ai.shreds.domain.exceptions.DomainSagaException if step processing fails
     */
    void processStep(DomainSagaStateEntity sagaState, SharedSagaStepEnum step);

    /**
     * Executes compensation logic for failed saga steps.
     * @param sagaState The saga state requiring compensation
     * @param failedStep The step that failed and triggered compensation
     * @throws ai.shreds.domain.exceptions.DomainSagaException if compensation fails
     */
    void compensate(DomainSagaStateEntity sagaState, SharedSagaStepEnum failedStep);

    /**
     * Handles saga timeout scenarios.
     * @param sagaState The timed-out saga state
     * @throws ai.shreds.domain.exceptions.DomainSagaException if timeout handling fails
     */
    void handleTimeout(DomainSagaStateEntity sagaState);
}