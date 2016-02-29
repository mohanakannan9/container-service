package org.nrg.containers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ContainerServerException extends Exception {
    public ContainerServerException(final String message) {
        super(message);
    }

    public ContainerServerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ContainerServerException(final Throwable cause) {
        super(cause);
    }
}