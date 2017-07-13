package org.nrg.containers.services.impl;

import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.services.CommandResolutionService;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.containers.services.ContainerFinalizeService;
import org.nrg.containers.services.ContainerService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.nrg.containers.model.command.entity.CommandType.DOCKER;

@Service
public class ContainerServiceImpl implements ContainerService {
    private static final Logger log = LoggerFactory.getLogger(ContainerServiceImpl.class);
    private static final Pattern exitCodePattern = Pattern.compile("kill|die|oom\\((\\d+|x)\\)");

    private final ContainerControlApi containerControlApi;
    private final ContainerEntityService containerEntityService;
    private final CommandResolutionService commandResolutionService;
    private final AliasTokenService aliasTokenService;
    private final SiteConfigPreferences siteConfigPreferences;
    private final ContainerFinalizeService containerFinalizeService;

    @Autowired
    public ContainerServiceImpl(final ContainerControlApi containerControlApi,
                                final ContainerEntityService containerEntityService,
                                final CommandResolutionService commandResolutionService,
                                final AliasTokenService aliasTokenService,
                                final SiteConfigPreferences siteConfigPreferences,
                                final ContainerFinalizeService containerFinalizeService) {
        this.containerControlApi = containerControlApi;
        this.containerEntityService = containerEntityService;
        this.commandResolutionService = commandResolutionService;
        this.aliasTokenService = aliasTokenService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.containerFinalizeService = containerFinalizeService;
    }

    @Override
    @Nonnull
    public ContainerEntity resolveCommandAndLaunchContainer(final long wrapperId,
                                                            final Map<String, String> inputValues,
                                                            final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public ContainerEntity resolveCommandAndLaunchContainer(final long commandId,
                                                            final String wrapperName,
                                                            final Map<String, String> inputValues,
                                                            final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public ContainerEntity resolveCommandAndLaunchContainer(final String project,
                                                            final long wrapperId,
                                                            final Map<String, String> inputValues,
                                                            final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public ContainerEntity resolveCommandAndLaunchContainer(final String project,
                                                            final long commandId,
                                                            final String wrapperName,
                                                            final Map<String, String> inputValues,
                                                            final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public ContainerEntity launchResolvedCommand(final ResolvedCommand resolvedCommand,
                                                 final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException, UnsupportedOperationException {
        if (resolvedCommand.type().equals(DOCKER.getName())) {
            return launchResolvedDockerCommand(resolvedCommand, userI);
        } else {
            throw new UnsupportedOperationException("Cannot launch a command of type " + resolvedCommand.type());
        }
    }

    @Nonnull
    private ContainerEntity launchResolvedDockerCommand(final ResolvedCommand resolvedCommand,
                                                        final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException {
        log.info("Preparing to launch resolved command.");
        final ResolvedCommand preparedToLaunch = prepareToLaunch(resolvedCommand, userI);

        log.info("Creating container from resolved command.");
        final String containerId = containerControlApi.createContainer(preparedToLaunch);

        log.info("Recording container launch.");
        final ContainerEntity containerEntity = containerEntityService.save(preparedToLaunch, containerId, userI);
        containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromUserAction("Created", userI.getLogin()));

        log.info("Starting container.");
        try {
            containerControlApi.startContainer(containerId);
        } catch (DockerServerException e) {
            containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromSystem("Did not start"));
            handleFailure(containerEntity);
            throw new ContainerException("Failed to start");
        }

        return containerEntity;
    }

    @Nonnull
    private ResolvedCommand prepareToLaunch(final ResolvedCommand resolvedCommand,
                                            final UserI userI) {
        final ResolvedCommand.Builder resolvedCommandBuilder =
                resolvedCommand.toBuilder();

        // Add default environment variables & create new alias token
        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        final String processingUrl = (String)siteConfigPreferences.getProperty("processingUrl", siteConfigPreferences.getSiteUrl());
        resolvedCommandBuilder.addEnvironmentVariable("XNAT_USER", token.getAlias())
                .addEnvironmentVariable("XNAT_PASS", token.getSecret())
                .addEnvironmentVariable("XNAT_HOST", processingUrl);

        return resolvedCommandBuilder.build();
    }




    @Override
    @Transactional
    public void processEvent(final ContainerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Processing container event");
        }
        final ContainerEntity execution = containerEntityService.addContainerEventToHistory(event);


        // execution will be null if either we aren't tracking the container
        // that this event is about, or if we have already recorded the event
        if (execution != null ) {

            final Matcher exitCodeMatcher =
                    exitCodePattern.matcher(event.getStatus());
            if (exitCodeMatcher.matches()) {
                log.debug("Container is dead. Finalizing.");
                final String exitCode = exitCodeMatcher.group(1);
                final String userLogin = execution.getUserId();
                try {
                    final UserI userI = Users.getUser(userLogin);
                    finalize(execution, userI, exitCode);
                } catch (UserInitException | UserNotFoundException e) {
                    log.error("Could not finalize container execution. Could not get user details for user " + userLogin, e);
                }

            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Done processing docker container event: " + event);
        }
    }

    @Override
    @Transactional
    public void finalize(final String containerId, final UserI userI) throws NotFoundException {
        final ContainerEntity containerEntity = containerEntityService.get(containerId);
        finalize(containerEntity, userI);
    }

    @Override
    @Transactional
    public void finalize(final ContainerEntity containerEntity, final UserI userI) {
        String exitCode = "x";
        for (final ContainerEntityHistory history : containerEntity.getHistory()) {
            final Matcher exitCodeMatcher = exitCodePattern.matcher(history.getStatus());
            if (exitCodeMatcher.matches()) {
                exitCode = exitCodeMatcher.group(1);
            }
        }
        finalize(containerEntity, userI, exitCode);
    }

    @Override
    @Transactional
    public void finalize(final ContainerEntity containerEntity, final UserI userI, final String exitCode) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Finalizing ContainerExecution %s for container %s", containerEntity.getId(), containerEntity.getContainerId()));
        }

        containerFinalizeService.finalizeContainer(containerEntity, userI, exitCode);

        if (log.isInfoEnabled()) {
            log.info(String.format("Done uploading for ContainerExecution %s. Now saving information about created outputs.", containerEntity.getId()));
        }
        containerEntityService.update(containerEntity);
        if (log.isDebugEnabled()) {
            log.debug("Done saving outputs for Container " + String.valueOf(containerEntity.getId()));
        }
    }

    @Override
    @Nonnull
    @Transactional
    public String kill(final String containerId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        // TODO check user permissions. How?
        final ContainerEntity containerEntity = containerEntityService.get(containerId);

        containerEntityService.addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromUserAction("Killed", userI.getLogin()));

        final String containerDockerId = containerEntity.getContainerId();
        containerControlApi.killContainer(containerDockerId);
        return containerDockerId;
    }

    private void handleFailure(final ContainerEntity containerEntity) {
        // TODO handle failure
    }
}
