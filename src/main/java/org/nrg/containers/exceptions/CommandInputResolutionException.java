package org.nrg.containers.exceptions;

import org.nrg.containers.model.CommandInputEntity;

public class CommandInputResolutionException extends CommandResolutionException {
    private final CommandInputEntity input;

    public CommandInputResolutionException(final String message, final CommandInputEntity input) {
        super(message);
        this.input = input;
    }

    public CommandInputResolutionException(final String message, final CommandInputEntity input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public CommandInputEntity getInput() {
        return input;
    }
}
