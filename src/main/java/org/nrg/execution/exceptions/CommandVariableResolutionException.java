package org.nrg.execution.exceptions;

import org.nrg.execution.model.CommandInput;

public class CommandVariableResolutionException extends Exception {
    final CommandInput variable;

    public CommandVariableResolutionException(final String message, final CommandInput variable) {
        super(message);
        this.variable = variable;
    }

    public CommandVariableResolutionException(final String message, final CommandInput variable, final Throwable cause) {
        super(message, cause);
        this.variable = variable;
    }

    public CommandVariableResolutionException(final CommandInput variable) {
        super(String.format("Variable \"%s\" has no provided or default value, but is required.", variable.getName()));
        this.variable = variable;
    }

    public CommandInput getVariable() {
        return variable;
    }
}
