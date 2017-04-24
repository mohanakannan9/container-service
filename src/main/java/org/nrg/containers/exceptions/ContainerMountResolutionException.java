package org.nrg.containers.exceptions;

import org.nrg.containers.model.command.auto.ResolvedCommand.PartiallyResolvedCommandMount;

public class ContainerMountResolutionException extends CommandResolutionException {
    final PartiallyResolvedCommandMount mount;

    public ContainerMountResolutionException(final String message, final PartiallyResolvedCommandMount mount) {
        super(message);
        this.mount = mount;
    }

    public ContainerMountResolutionException(final String message, final PartiallyResolvedCommandMount mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public PartiallyResolvedCommandMount getMount() {
        return mount;
    }
}
