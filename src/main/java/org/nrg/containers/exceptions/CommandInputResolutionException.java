package org.nrg.containers.exceptions;

import org.nrg.containers.model.CommandInput;

public class CommandInputResolutionException extends CommandResolutionException {
    final CommandInput input;

    public CommandInputResolutionException(final String message, final CommandInput input) {
        super(message);
        this.input = input;
    }

    public CommandInputResolutionException(final String message, final CommandInput input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public CommandInput getInput() {
        return input;
    }
}
