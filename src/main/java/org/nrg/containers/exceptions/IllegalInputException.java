package org.nrg.containers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class IllegalInputException extends CommandResolutionException {
    public IllegalInputException(final String message) {
        super(message);
    }

    public IllegalInputException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IllegalInputException(final Throwable cause) {
        super(cause);
    }
}