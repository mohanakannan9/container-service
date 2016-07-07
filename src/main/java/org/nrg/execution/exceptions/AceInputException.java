package org.nrg.execution.exceptions;

import org.nrg.execution.model.ActionInput;

public class AceInputException extends Exception {
    final ActionInput input;

    public AceInputException(final String message, final ActionInput input) {
        super(message);
        this.input = input;
    }

    public AceInputException(final String message, final ActionInput input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public AceInputException(final ActionInput input) {
        super(String.format("Input \"%s\" has no provided or default value, but is required.", input.getName()));
        this.input = input;
    }

    public ActionInput getInput() {
        return input;
    }
}
