package org.nrg.containers.services;

import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface ContainerService {
    Container save(final ResolvedCommand resolvedCommand,
                   final String containerId,
                   final UserI userI);

    List<Container> getAll();
    Container retrieve(final long id);
    Container retrieve(final String containerId);
    Container get(final long id) throws NotFoundException;
    Container get(final String containerId) throws NotFoundException;
    void delete(final long id) throws NotFoundException;
    void delete(final String containerId) throws NotFoundException;

    Container addContainerEventToHistory(final ContainerEvent containerEvent);
    Container.ContainerHistory addContainerHistoryItem(final Container container,
                                                       final Container.ContainerHistory history);

    Container resolveCommandAndLaunchContainer(long wrapperId,
                                               Map<String, String> inputValues,
                                               UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    Container resolveCommandAndLaunchContainer(long commandId,
                                               String wrapperName,
                                               Map<String, String> inputValues,
                                               UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    Container resolveCommandAndLaunchContainer(String project,
                                               long wrapperId,
                                               Map<String, String> inputValues,
                                               UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    Container resolveCommandAndLaunchContainer(String project,
                                               long commandId,
                                               String wrapperName,
                                               Map<String, String> inputValues,
                                               UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException;
    Container launchResolvedCommand(final ResolvedCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException;

    void processEvent(final ContainerEvent event);

    void finalize(final String containerId, final UserI userI) throws NotFoundException;
    void finalize(final Container container, final UserI userI);
    void finalize(final Container container, final UserI userI, final String exitCode);

    String kill(final String containerId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException;
}
