package org.nrg.actions.services;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.Context;
import org.nrg.xft.exception.XFTInitException;

import java.util.List;

public interface AceService {
    public List<ActionContextExecution> resolveAces(final Context context) throws XFTInitException;
}
