package org.nrg.execution.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.execution.api.ContainerControlApi;
import org.nrg.execution.daos.CommandDao;
import org.nrg.execution.exceptions.CommandInputResolutionException;
import org.nrg.execution.exceptions.CommandMountResolutionException;
import org.nrg.execution.exceptions.DockerServerException;
import org.nrg.execution.exceptions.NoServerPrefException;
import org.nrg.execution.exceptions.NotFoundException;
import org.nrg.execution.model.Command;
import org.nrg.execution.model.CommandMount;
import org.nrg.execution.model.CommandRun;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.exceptions.NrgRuntimeException;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.transporter.TransportService;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
    private static final Logger log = LoggerFactory.getLogger(HibernateCommandService.class);

//    @Autowired private ObjectMapper mapper;
    @Autowired private ContainerControlApi controlApi;
    @Autowired private AliasTokenService aliasTokenService;
    @Autowired private SiteConfigPreferences siteConfigPreferences;
    @Autowired private TransportService transporter;
    @Autowired private ContainerExecutionService containerExecutionService;

    @Override
    public Command get(final Long id) throws NotFoundException {
        final Command command = retrieve(id);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + id);
        }
        return command;
    }

    @Override
    public void initialize(final Command command) {
        if (command == null) {
            return;
        }
        Hibernate.initialize(command);
        Hibernate.initialize(command.getInputs());
        Hibernate.initialize(command.getOutputs());

        final CommandRun run = command.getRun();
        if (run != null) {
            Hibernate.initialize(run.getEnvironmentVariables());
            Hibernate.initialize(run.getCommandLine());
            Hibernate.initialize(run.getMounts());
        }
    }

    @Override
    public Command create(final Command command) throws NrgRuntimeException {
        try {
            return super.create(command);
        } catch (ConstraintViolationException e) {
            throw new NrgServiceRuntimeException("A command already exists with this name and docker image ID.");
        }
    }

    @Override
    public List<Command> save(final List<Command> commands) {
        final List<Command> saved = Lists.newArrayList();
        if (!(commands == null || commands.isEmpty())) {
            for (final Command command : commands) {
                try {
                    create(command);
                    saved.add(command);
                } catch (NrgServiceRuntimeException e) {
                    // TODO: should I "update" instead of erroring out if command already exists?
                    log.error("Could not save command: " + command, e);
                }
            }
        }
        getDao().flush();
        return saved;
    }

    @Override
    public ResolvedCommand resolveCommand(final Long commandId,
                                          final Map<String, String> variableValuesProvidedAtRuntime,
                                          final UserI userI)
            throws NotFoundException, CommandInputResolutionException, CommandMountResolutionException {
        final Command command = get(commandId);
        return resolveCommand(command, variableValuesProvidedAtRuntime, userI);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command, final UserI userI)
            throws NotFoundException, CommandInputResolutionException, CommandMountResolutionException {
        return CommandResolutionHelper.resolve(command, userI);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> inputValuesProvidedAtRuntime,
                                          final UserI userI)
            throws NotFoundException, CommandInputResolutionException, CommandMountResolutionException {
        return CommandResolutionHelper.resolve(command, inputValuesProvidedAtRuntime, userI);
    }

    @Override
    public ContainerExecution launchCommand(final ResolvedCommand resolvedCommand, final UserI userI)
            throws NoServerPrefException, DockerServerException {
        return launchCommand(resolvedCommand, Maps.<String, String>newHashMap(), userI);
    }

    @Override
    public ContainerExecution launchCommand(final ResolvedCommand resolvedCommand,
                                            final Map<String, String> inputValues,
                                            final UserI userI)
            throws NoServerPrefException, DockerServerException {
        final ResolvedCommand preparedToLaunch = prelaunch(resolvedCommand, userI);
        final String containerId = controlApi.launchImage(preparedToLaunch);
        return containerExecutionService.save(preparedToLaunch, containerId, inputValues, userI);
    }

    @Override
    public ContainerExecution launchCommand(final Long commandId, final Map<String, String> runtimeValues, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandInputResolutionException, CommandMountResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, runtimeValues, userI);
        return launchCommand(resolvedCommand, runtimeValues, userI);
    }

    private ResolvedCommand prelaunch(final ResolvedCommand resolvedCommand, final UserI userI) throws NoServerPrefException {
        // Add default environment variables
        final Map<String, String> defaultEnv = Maps.newHashMap();
//        siteConfigPreferences.getBuildPath()
        defaultEnv.put("XNAT_HOST", siteConfigPreferences.getSiteUrl());

        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        defaultEnv.put("XNAT_USER", token.getAlias());
        defaultEnv.put("XNAT_PASS", token.getSecret());

        resolvedCommand.addEnvironmentVariables(defaultEnv);

        // Transport mounts
        if (resolvedCommand.getMountsIn() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            for (final CommandMount mountIn : resolvedCommand.getMountsIn()) {
                final Path pathOnXnatHost = Paths.get(mountIn.getHostPath());
                final Path pathOnDockerHost = transporter.transport(dockerHost, pathOnXnatHost);
                mountIn.setHostPath(pathOnDockerHost.toString());
            }
        }
        if (resolvedCommand.getMountsOut() != null) {
            final String dockerHost = controlApi.getServer().getHost();
            final List<CommandMount> mountsOut = resolvedCommand.getMountsOut();
            final List<Path> buildPaths = transporter.getWritableDirectories(dockerHost, mountsOut.size());
            for (int i=0; i < mountsOut.size(); i++) {
                final CommandMount mountOut = mountsOut.get(i);
                final Path buildPath = buildPaths.get(i);

                mountOut.setHostPath(buildPath.toString());
            }
        }

        return resolvedCommand;
    }
}
