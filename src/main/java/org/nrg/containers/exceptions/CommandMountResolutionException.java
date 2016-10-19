package org.nrg.containers.exceptions;

import org.nrg.containers.model.CommandMount;

public class CommandMountResolutionException extends CommandResolutionException {
    final CommandMount mount;

    public CommandMountResolutionException(final String message, final CommandMount mount) {
        super(message);
        this.mount = mount;
    }

    public CommandMountResolutionException(final String message, final CommandMount mount, final Throwable cause) {
        super(message, cause);
        this.mount = mount;
    }

    public CommandMount getMount() {
        return mount;
    }
}
