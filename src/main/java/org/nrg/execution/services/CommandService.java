package org.nrg.execution.services;

import org.nrg.execution.exceptions.CommandVariableResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {

    ResolvedCommand resolveCommand(final Long commandId) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command) throws NotFoundException, CommandVariableResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues)
            throws NotFoundException, CommandVariableResolutionException;

    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand, final UserI userI) throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand,
                                     final String rootObjectId,
                                     final String rootObjectXsiType,
                                     final UserI userI)
            throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final Long commandId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;
    ContainerExecution launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandVariableResolutionException;

    ContainerExecution launchCommand(Long commandId, UserI userI, XnatImagesessiondata session)
            throws NotFoundException, CommandVariableResolutionException, NoServerPrefException,
            DockerServerException, XFTInitException;
    ContainerExecution launchCommand(Long commandId, UserI userI, XnatImagesessiondata session, XnatImagescandata scan)
            throws NotFoundException, XFTInitException, CommandVariableResolutionException, NoServerPrefException, DockerServerException;

    List<Command> save(final List<Command> commands);
}
