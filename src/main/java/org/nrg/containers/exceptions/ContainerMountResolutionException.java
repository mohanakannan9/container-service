package org.nrg.containers.exceptions;

import org.nrg.containers.model.command.auto.ResolvedCommand.ResolvedCommandMount;

public class ContainerMountResolutionException extends CommandResolutionException {
    final ResolvedCommandMount mount;

    public ContainerMountResolutionException(final String message, final ResolvedCommandMount mount) {
        super(message);
        this.mount = mount;
    }

    public ContainerMountResolutionException(final String message, final ResolvedCommandMount mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public ResolvedCommandMount getMount() {
        return mount;
    }
}
