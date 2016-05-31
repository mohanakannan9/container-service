package org.nrg.actions.services;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ActionContextExecutionDto;
import org.nrg.actions.model.Context;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.exception.XFTInitException;

import java.util.List;

public interface AceService extends BaseHibernateService<ActionContextExecution> {
    List<ActionContextExecutionDto> resolveAces(final Context context) throws XFTInitException;

    ActionContextExecution launchAce(final ActionContextExecutionDto ace) throws NotFoundException;
}
