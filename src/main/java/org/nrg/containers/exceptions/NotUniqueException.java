package org.nrg.containers.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NotUniqueException extends Exception {
    public NotUniqueException() {
        super();
    }

    public NotUniqueException(final Throwable e) {
        super(e);
    }

    public NotUniqueException(final String message) {
        super(message);
    }
}
