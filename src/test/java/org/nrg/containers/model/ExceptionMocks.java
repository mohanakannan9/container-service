package org.nrg.containers.model;

import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;

@SuppressWarnings("ThrowableInstanceNeverThrown")
public class ExceptionMocks {
    public static final NotFoundException NOT_FOUND_EXCEPTION = new NotFoundException("Some cool message");
    public static final NoServerPrefException NO_SERVER_PREF_EXCEPTION = new NoServerPrefException("message");
}
