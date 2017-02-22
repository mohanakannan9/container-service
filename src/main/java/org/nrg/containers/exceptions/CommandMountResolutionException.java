package org.nrg.containers.exceptions;

import org.nrg.containers.model.CommandMountEntity;

public class CommandMountResolutionException extends CommandResolutionException {
    final CommandMountEntity mount;

    public CommandMountResolutionException(final String message, final CommandMountEntity mount) {
        super(message);
        this.mount = mount;
    }

    public CommandMountResolutionException(final String message, final CommandMountEntity mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public CommandMountEntity getMount() {
        return mount;
    }
}
