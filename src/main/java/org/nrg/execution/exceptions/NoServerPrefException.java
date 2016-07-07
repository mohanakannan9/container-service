package org.nrg.execution.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
public class NoServerPrefException extends Exception {
    public NoServerPrefException(final String message) {
        super(message);
    }

    public NoServerPrefException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NoServerPrefException(final Throwable cause) {
        super(cause);
    }
}