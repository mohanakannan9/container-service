package org.nrg.containers.exceptions;

import org.nrg.containers.model.auto.Command.CommandWrapperInput;

public class CommandWrapperInputResolutionException extends CommandResolutionException {
    private final CommandWrapperInput input;

    public CommandWrapperInputResolutionException(final String message, final CommandWrapperInput input) {
        super(message);
        this.input = input;
    }

    public CommandWrapperInputResolutionException(final String message, final CommandWrapperInput input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public CommandWrapperInput getInput() {
        return input;
    }
}
