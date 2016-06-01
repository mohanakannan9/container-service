package org.nrg.actions.services;

import org.nrg.actions.model.Command;
import org.nrg.actions.model.ResolvedCommand;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    ResolvedCommand resolveCommand(final Long commandId) throws NotFoundException;
    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException;
    ResolvedCommand resolveCommand(final Command command) throws NotFoundException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException;

    String launchCommand(final ResolvedCommand resolvedCommand) throws NoServerPrefException, DockerServerException;
    String launchCommand(final Long commandId)
            throws NoServerPrefException, DockerServerException, NotFoundException;
    String launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues)
            throws NoServerPrefException, DockerServerException, NotFoundException;
}
