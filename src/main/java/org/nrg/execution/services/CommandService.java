package org.nrg.execution.services;

import org.nrg.execution.exceptions.CommandInputResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<Command> {
    Command get(Long id) throws NotFoundException;

    ResolvedCommand resolveCommand(final Long commandId,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandInputResolutionException;
    ResolvedCommand resolveCommand(final Command command, final UserI userI)
            throws NotFoundException, CommandInputResolutionException;
    ResolvedCommand resolveCommand(final Command command,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandInputResolutionException;

    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand, final UserI userI) throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final ResolvedCommand resolvedCommand,
                                     final Map<String, String> inputValues,
                                     final UserI userI)
            throws NoServerPrefException, DockerServerException;
    ContainerExecution launchCommand(final Long commandId,
                         final Map<String, String> variableRuntimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandInputResolutionException;

    List<Command> save(final List<Command> commands);

//    @VisibleForTesting
//    ResolvedCommand prepareToLaunchScan(Command command,
//                                        XnatImagesessiondata session,
//                                        XnatImagescandata scan,
//                                        UserI userI)
//            throws CommandInputResolutionException, NotFoundException, XFTInitException, NoServerPrefException;
}
