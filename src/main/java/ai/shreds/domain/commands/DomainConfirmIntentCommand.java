package ai.shreds.domain.commands;

import ai.shreds.domain.value_objects.DomainPaymentIntentIdValue;
import java.util.Objects;

/**
 * Command to confirm a payment intent in the domain layer.
 */
public class DomainConfirmIntentCommand {
    private final DomainPaymentIntentIdValue intentId;
    private final String clientSecret;

    public DomainConfirmIntentCommand(
            DomainPaymentIntentIdValue intentId,
            String clientSecret) {
        this.intentId = Objects.requireNonNull(intentId, "intentId cannot be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret cannot be null");
        
        validateCommand();
    }

    private void validateCommand() {
        if (clientSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("clientSecret cannot be empty");
        }
    }

    public DomainPaymentIntentIdValue getIntentId() {
        return intentId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DomainConfirmIntentCommand)) return false;
        DomainConfirmIntentCommand that = (DomainConfirmIntentCommand) o;
        return intentId.equals(that.intentId) && clientSecret.equals(that.clientSecret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentId, clientSecret);
    }

    @Override
    public String toString() {
        return "DomainConfirmIntentCommand{" +
                "intentId=" + intentId +
                ", clientSecret=*** (hidden for security)" +
                '}';
    }
}