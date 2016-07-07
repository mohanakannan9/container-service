package org.nrg.execution.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class NoHubException extends Exception {
    public NoHubException(final String message) {
        super(message);
    }

    public NoHubException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NoHubException(final Throwable cause) {
        super(cause);
    }
}
