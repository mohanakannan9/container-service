package org.nrg.execution.services;

import org.nrg.execution.model.Command;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    List<Command> findByName(final String name);

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
