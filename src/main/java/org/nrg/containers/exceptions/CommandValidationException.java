package org.nrg.containers.exceptions;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.List;

public class CommandValidationException extends Exception {
    private ImmutableList<String> errors;

    public CommandValidationException(final String name, final List<String> errors) {
        super();
        setErrors(name, errors);
    }

    public CommandValidationException(final String name, final String error) {
        super();
        setErrors(name, error);
    }

    public CommandValidationException(final String name, final List<String> errors, final Throwable e) {
        super(e);
        setErrors(name, errors);
    }

    private void setErrors(final String name, final List<String> errors) {
        final String prefix = "Command " + name + ": ";
        final Function<String, String> addPrefix = new Function<String, String>() {
            @Nullable
            @Override
            public String apply(@Nullable final String s) {
                return prefix + s;
            }
        };
        this.errors = errors == null ? ImmutableList.of(prefix) : ImmutableList.copyOf(Lists.transform(errors, addPrefix));
    }

    private void setErrors(final String name, final String error) {
        final String prefix = "Command " + name + ": ";
        this.errors = error == null ? ImmutableList.of(prefix) : ImmutableList.of(prefix + error);
    }

    public List<String> getErrors() {
        return errors;
    }
}
