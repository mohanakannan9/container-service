package org.nrg.execution.exceptions;

import org.nrg.execution.model.CommandVariable;

public class CommandVariableResolutionException extends Exception {
    final CommandVariable variable;

    public CommandVariableResolutionException(final String message, final CommandVariable variable) {
        super(message);
        this.variable = variable;
    }

    public CommandVariableResolutionException(final String message, final CommandVariable variable, final Throwable cause) {
        super(message, cause);
        this.variable = variable;
    }

    public CommandVariableResolutionException(final CommandVariable variable) {
        super(String.format("Variable \"%s\" has no provided or default value, but is required.", variable.getName()));
        this.variable = variable;
    }

    public CommandVariable getVariable() {
        return variable;
    }
}
