package org.nrg.containers.services;

import org.nrg.containers.exceptions.*;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    Command get(Long id) throws NotFoundException;

    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final Command command, final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;

    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand, final UserI userI) throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand,
                                     final Map<String, String> inputValues,
                                     final UserI userI)
            throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException;

    List<Command> save(final List<Command> commands);

//    @VisibleForTesting
//    ResolvedCommand prepareToLaunchScan(Command command,
//                                        XnatImagesessiondata session,
//                                        XnatImagescandata scan,
//                                        UserI userI)
//            throws CommandInputResolutionException, NotFoundException, XFTInitException, NoServerPrefException;
}
