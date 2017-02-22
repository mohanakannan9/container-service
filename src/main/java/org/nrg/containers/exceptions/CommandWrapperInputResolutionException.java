package org.nrg.containers.exceptions;

import org.nrg.containers.model.CommandWrapperInputEntity;

public class CommandWrapperInputResolutionException extends CommandResolutionException {
    private final CommandWrapperInputEntity input;

    public CommandWrapperInputResolutionException(final String message, final CommandWrapperInputEntity input) {
        super(message);
        this.input = input;
    }

    public CommandWrapperInputResolutionException(final String message, final CommandWrapperInputEntity input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public CommandWrapperInputEntity getInput() {
        return input;
    }
}
