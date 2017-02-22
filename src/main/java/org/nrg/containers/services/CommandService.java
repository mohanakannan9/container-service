package org.nrg.containers.services;

import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.CommandValidationException;
import org.nrg.containers.exceptions.ContainerMountResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.CommandEntity;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.CommandWrapperEntity;
import org.nrg.containers.model.auto.Command;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

public interface CommandService extends BaseHibernateService<CommandEntity> {
    List<Command> getAllCommands();
    Command create(Command command) throws CommandValidationException;

    List<CommandEntity> findByProperties(Map<String, Object> properties);
    List<Command> findCommandByProperties(Map<String, Object> properties);
    CommandEntity get(Long id) throws NotFoundException;
    Command getCommand(Long id) throws NotFoundException;

    CommandEntity update(Long id, CommandEntity updates, Boolean ignoreNull) throws NotFoundException;
    Command update(Long id, Command updates, Boolean ignoreNull) throws NotFoundException, CommandValidationException;

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
    ResolvedCommand resolveCommand(final CommandEntity commandEntity,
                                   final Map<String, String> variableRuntimeValues,
                                   final UserI userI)
            throws NotFoundException, CommandResolutionException;
    ResolvedCommand resolveCommand(final CommandWrapperEntity commandWrapperEntity,
                                   final CommandEntity commandEntity,
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

    List<Command> save(final List<Command> commands);

//    @VisibleForTesting
//    ResolvedCommand prepareToLaunchScan(Command command,
//                                        XnatImagesessiondata session,
//                                        XnatImagescandata scan,
//                                        UserI userI)
//            throws CommandInputResolutionException, NotFoundException, XFTInitException, NoServerPrefException;
}
