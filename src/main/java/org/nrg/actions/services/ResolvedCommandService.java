package org.nrg.actions.services;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface ResolvedCommandService
        extends BaseHibernateService<ResolvedCommand> {
    ResolvedCommand resolveCommand(ActionContextExecution ace) throws NotFoundException;
}
