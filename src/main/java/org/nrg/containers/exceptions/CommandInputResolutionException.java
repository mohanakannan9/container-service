package org.nrg.containers.exceptions;

import org.nrg.containers.model.command.auto.Command.Input;

public class CommandInputResolutionException extends CommandResolutionException {
    private final Input input;
    private final String value;

    public CommandInputResolutionException(final String message, final Throwable cause) {
        super(message, cause);
        this.input = null;
        this.value = null;
    }

    public CommandInputResolutionException(final String message, final Input input) {
        super(message);
        this.input = input;
        this.value = null;
    }

    public CommandInputResolutionException(final String message, final Input input, final Throwable cause) {
        super(message, cause);
        this.input = input;
        this.value = null;
    }

    public CommandInputResolutionException(final String message, final String value) {
        super(message);
        this.input = null;
        this.value = value;
    }

    public CommandInputResolutionException(final String message, final String value, final Throwable cause) {
        super(message, cause);
        this.input = null;
        this.value = value;
    }

    public CommandInputResolutionException(final String message, final Input input, final String value) {
        super(message);
        this.input = input;
        this.value = value;
    }

    public CommandInputResolutionException(final String message, final Input input, final String value, final Throwable cause) {
        super(message, cause);
        this.input = input;
        this.value = value;
    }

    public Input getInput() {
        return input;
    }
    public String getValue() {
        return value;
    }
}
