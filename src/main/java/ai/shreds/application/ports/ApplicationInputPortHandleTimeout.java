package ai.shreds.application.ports;

import ai.shreds.application.dtos.ApplicationTimeoutResultDTO;

/**
 * Input port for handling saga timeouts.
 */
public interface ApplicationInputPortHandleTimeout {

    /**
     * Process stale sagas and trigger timeout handling.
     *
     * @return result summary of timeout processing
     */
    ApplicationTimeoutResultDTO handleTimeouts();
}
