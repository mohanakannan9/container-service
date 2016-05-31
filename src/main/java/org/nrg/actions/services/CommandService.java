package org.nrg.actions.services;

import org.nrg.actions.model.ActionContextExecution;
import org.nrg.actions.model.Command;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface CommandService extends BaseHibernateService<Command> {
    ResolvedCommand resolveCommand(ActionContextExecution ace) throws NotFoundException;
}
