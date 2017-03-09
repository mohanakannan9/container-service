package org.nrg.containers.services;

import org.nrg.containers.events.DockerContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.auto.Command;
import org.nrg.containers.model.auto.Command.CommandWrapper;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface ContainerService {
    ResolvedCommand resolveCommand(final long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final String xnatCommandWrapperName,
                                   final long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final long xnatCommandWrapperId,
                                   final long commandId,
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

    ContainerEntity resolveAndLaunchCommand(final long commandId,
                                            final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException;
    ContainerEntity resolveAndLaunchCommand(final String xnatCommandWrapperName,
                                            final long commandId,
                                            final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException;
    ContainerEntity resolveAndLaunchCommand(final long xnatCommandWrapperId,
                                            final long commandId,
                                            final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException;
    ContainerEntity launchResolvedDockerCommand(final ResolvedDockerCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerMountResolutionException, ContainerException;

    void processEvent(final DockerContainerEvent event);

    void finalize(final Long containerExecutionId, final UserI userI);
    void finalize(final ContainerEntity containerEntity, final UserI userI, final String exitCode);

    String kill(final Long containerExecutionId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException;
}
