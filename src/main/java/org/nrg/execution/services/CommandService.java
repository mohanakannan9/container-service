package org.nrg.execution.services;

import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    List<Command> findByName(final String name);

    ResolvedCommand resolveCommand(final Long commandId) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;

    String launchCommand(final ResolvedCommand resolvedCommand) throws NoServerPrefException, DockerServerException;
    String launchCommand(final Long commandId)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;
    String launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;
}
