package com.studybuddy.common.error;

/**
 * A broken internal rule, such as missing wiring or a call made outside a transaction. It is a
 * fault in the application, never something the user did, so it is deliberately not an
 * {@link IllegalStateException} (which the API reports as a 409 state conflict) and falls
 * through to the 500 handler instead.
 */
public class InternalInvariantException extends RuntimeException {

    public InternalInvariantException(String message) {
        super(message);
    }

    public InternalInvariantException(String message, Throwable cause) {
        super(message, cause);
    }
}
