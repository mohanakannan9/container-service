package org.nrg.containers.exceptions;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class CommandValidationException extends Exception {
    private ImmutableList<String> errors;

    public CommandValidationException(final List<String> errors) {
        super();
        this.errors = errors == null ? ImmutableList.<String>of() : ImmutableList.copyOf(errors);
    }

    public CommandValidationException(final List<String> errors, final Throwable e) {
        super(e);
        this.errors = errors == null ? ImmutableList.<String>of() : ImmutableList.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
