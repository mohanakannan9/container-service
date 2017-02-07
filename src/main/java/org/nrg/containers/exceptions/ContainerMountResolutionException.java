package org.nrg.containers.exceptions;

import org.nrg.containers.model.ContainerExecutionMount;

public class ContainerMountResolutionException extends CommandResolutionException {
    final ContainerExecutionMount mount;

    public ContainerMountResolutionException(final String message, final ContainerExecutionMount mount) {
        super(message);
        this.mount = mount;
    }

    public ContainerMountResolutionException(final String message, final ContainerExecutionMount mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public ContainerExecutionMount getMount() {
        return mount;
    }
}
