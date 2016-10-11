package org.nrg.execution.exceptions;

import org.nrg.execution.model.CommandInput;

public class CommandInputResolutionException extends Exception {
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
