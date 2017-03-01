package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface ContainerLaunchService {
    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final String xnatCommandWrapperName,
                                   final Long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final Long xnatCommandWrapperId,
                                   final Long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final CommandWrapper commandWrapper,
                                   final Command command,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;

    ContainerExecution resolveAndLaunchCommand(final Long commandId,
                                               final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException;
    ContainerExecution resolveAndLaunchCommand(final String xnatCommandWrapperName,
                                               final Long commandId,
                                               final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException;
    ContainerExecution resolveAndLaunchCommand(final Long xnatCommandWrapperId,
                                               final Long commandId,
                                               final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException;
    ContainerExecution launchResolvedDockerCommand(final ResolvedDockerCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException;
}
