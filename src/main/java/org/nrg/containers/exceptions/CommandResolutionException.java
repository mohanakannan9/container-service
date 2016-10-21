package org.nrg.containers.exceptions;

public class CommandResolutionException extends Exception {
    public CommandResolutionException(final String message) {
        super(message);
    }

    public CommandResolutionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
