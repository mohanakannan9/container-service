package org.nrg.containers.exceptions;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CommandValidationException extends Exception {
    private ImmutableList<String> errors;

    public CommandValidationException(final List<String> errors) {
        super();
        setErrors(errors);
    }

    public CommandValidationException(final String error) {
        super();
        this.errors = error == null ? ImmutableList.<String>of() : ImmutableList.of(error);
    }

    public CommandValidationException(final List<String> errors, final Throwable e) {
        super(e);
        setErrors(errors);
    }

    private void setErrors(final List<String> errors) {
        this.errors = errors == null ? ImmutableList.<String>of() : ImmutableList.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
