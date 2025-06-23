package ai.shreds.application.dtos;

import java.util.UUID;

/**
 * Command DTO for confirming payment intents in the application layer
 */
public class ApplicationConfirmIntentCommand {

    private UUID intentId;
    private String clientSecret;

    public ApplicationConfirmIntentCommand() {}

    public ApplicationConfirmIntentCommand(UUID intentId, String clientSecret) {
        this.intentId = intentId;
        this.clientSecret = clientSecret;
    }

    public UUID getIntentId() {
        return intentId;
    }

    public void setIntentId(UUID intentId) {
        this.intentId = intentId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}