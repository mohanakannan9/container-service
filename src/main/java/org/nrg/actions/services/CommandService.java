package org.nrg.actions.services;

import org.nrg.actions.model.Command;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    ResolvedCommand resolveCommand(final Long id,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException;
}
