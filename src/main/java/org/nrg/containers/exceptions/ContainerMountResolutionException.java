package org.nrg.containers.exceptions;

import org.nrg.containers.model.ContainerEntityMount;

public class ContainerMountResolutionException extends CommandResolutionException {
    final ContainerEntityMount mount;

    public ContainerMountResolutionException(final String message, final ContainerEntityMount mount) {
        super(message);
        this.mount = mount;
    }

    public ContainerMountResolutionException(final String message, final ContainerEntityMount mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public ContainerEntityMount getMount() {
        return mount;
    }
}
