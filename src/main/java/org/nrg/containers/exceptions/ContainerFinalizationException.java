package org.nrg.containers.exceptions;

import org.nrg.containers.model.container.auto.Container;

public class ContainerFinalizationException extends ContainerException {
    public ContainerFinalizationException(final Container container, final String message) {
        super(message);
        this.container = container;
    }

    public ContainerFinalizationException(final Container container, final String message, final Throwable e) {
        super(message, e);
        this.container = container;
    }

    public ContainerFinalizationException(final Container container, final Throwable e) {
        super(e);
        this.container = container;
    }

    private Container container;

    public Container getContainer() {
        return container;
    }
}
