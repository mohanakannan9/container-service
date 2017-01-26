package org.nrg.containers.exceptions;

import org.nrg.containers.model.XnatCommandInput;

public class XnatCommandInputResolutionException extends CommandResolutionException {
    private final XnatCommandInput input;

    public XnatCommandInputResolutionException(final String message, final XnatCommandInput input) {
        super(message);
        this.input = input;
    }

    public XnatCommandInputResolutionException(final String message, final XnatCommandInput input, final Throwable cause) {
        super(message, cause);
        this.input = input;
    }

    public XnatCommandInput getInput() {
        return input;
    }
}
