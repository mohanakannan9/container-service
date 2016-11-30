package org.nrg.containers.exceptions;

public class ContainerException extends Exception {
    public ContainerException(final String message) {
        super(message);
    }

    public ContainerException(final String message, final Throwable e) {
        super(message, e);
    }

    public ContainerException(final Throwable e) {
        super(e);
    }
}
