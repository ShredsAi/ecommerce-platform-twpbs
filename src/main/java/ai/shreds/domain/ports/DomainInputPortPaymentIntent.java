package ai.shreds.domain.ports;

import ai.shreds.domain.commands.DomainConfirmIntentCommand;
import ai.shreds.domain.commands.DomainCreateIntentCommand;
import ai.shreds.domain.entities.DomainPaymentIntentEntity;

/**
 * Input port for payment intent operations in the domain.
 * This interface defines the primary actions that can be performed on payment intents.
 */
public interface DomainInputPortPaymentIntent {

    /**
     * Creates a new payment intent based on the provided command.
     * @param command the command containing the details for creating the intent
     * @return the created payment intent entity
     */
    DomainPaymentIntentEntity createIntent(DomainCreateIntentCommand command);

    /**
     * Confirms an existing payment intent based on the provided command.
     * @param command the command containing the confirmation details
     * @return the updated payment intent entity
     */
    DomainPaymentIntentEntity confirmIntent(DomainConfirmIntentCommand command);

    /**
     * Expires payment intents that have reached their expiration time.
     * This is typically called by a scheduled job.
     */
    void expireIntents();
}
