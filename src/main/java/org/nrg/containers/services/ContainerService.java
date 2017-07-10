package org.nrg.containers.services;

import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;

import java.util.Map;

public interface ContainerService {
    ContainerEntity resolveCommandAndLaunchContainer(long wrapperId,
                                                     Map<String, String> inputValues,
                                                     UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    ContainerEntity resolveCommandAndLaunchContainer(long commandId,
                                                     String wrapperName,
                                                     Map<String, String> inputValues,
                                                     UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    ContainerEntity resolveCommandAndLaunchContainer(String project,
                                                     long wrapperId,
                                                     Map<String, String> inputValues,
                                                     UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    ContainerEntity resolveCommandAndLaunchContainer(String project,
                                                     long commandId,
                                                     String wrapperName,
                                                     Map<String, String> inputValues,
                                                     UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    ContainerEntity launchResolvedCommand(final ResolvedCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException;

    void processEvent(final ContainerEvent event);

    void finalize(final String containerId, final UserI userI) throws NotFoundException;
    void finalize(final ContainerEntity containerEntity, final UserI userI);
    void finalize(final ContainerEntity containerEntity, final UserI userI, final String exitCode);

    String kill(final String containerId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException;
}
