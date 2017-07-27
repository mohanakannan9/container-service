package org.nrg.containers.services.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.ContainerException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.UnauthorizedException;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.Container.ContainerHistory;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
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
    public List<Container> getAll() {
        return toPojo(containerEntityService.getAll());
    }

    @Override
    public Container save(final ResolvedCommand resolvedCommand, final String containerId, final UserI userI) {
        return toPojo(containerEntityService.save(resolvedCommand, containerId, userI));
    }

    @Override
    @Nullable
    public Container retrieve(final String containerId) {
        final ContainerEntity containerEntity = containerEntityService.retrieve(containerId);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nullable
    public Container retrieve(final long id) {
        final ContainerEntity containerEntity = containerEntityService.retrieve(id);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nonnull
    public Container get(final long id) throws NotFoundException {
        return toPojo(containerEntityService.get(id));
    }

    @Override
    @Nonnull
    public Container get(final String containerId) throws NotFoundException {
        return toPojo(containerEntityService.get(containerId));
    }

    @Override
    public void delete(final long id) throws NotFoundException {
        containerEntityService.delete(id);
    }

    @Override
    public void delete(final String containerId) throws NotFoundException {
        containerEntityService.delete(containerId);
    }

    @Override
    @Nullable
    public Container addContainerEventToHistory(final ContainerEvent containerEvent) {
        final ContainerEntity containerEntity = containerEntityService.addContainerEventToHistory(containerEvent);
        return containerEntity == null ? null : toPojo(containerEntity);
    }

    @Override
    @Nullable
    public ContainerHistory addContainerHistoryItem(final Container container, final ContainerHistory history) {
        final ContainerEntityHistory containerEntityHistoryItem = containerEntityService.addContainerHistoryItem(fromPojo(container), fromPojo(history));
        return containerEntityHistoryItem == null ? null : toPojo(containerEntityHistoryItem);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final long wrapperId,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final long commandId,
                                                      final String wrapperName,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final String project,
                                                      final long wrapperId,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, wrapperId, inputValues, userI), userI);
    }

    @Override
    @Nonnull
    public Container resolveCommandAndLaunchContainer(final String project,
                                                      final long commandId,
                                                      final String wrapperName,
                                                      final Map<String, String> inputValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException, ContainerException, UnauthorizedException {
        return launchResolvedCommand(commandResolutionService.resolve(project, commandId, wrapperName, inputValues, userI), userI);

    }

    @Override
    @Nonnull
    public Container launchResolvedCommand(final ResolvedCommand resolvedCommand,
                                           final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException, UnsupportedOperationException {
        if (resolvedCommand.type().equals(DOCKER.getName())) {
            return launchResolvedDockerCommand(resolvedCommand, userI);
        } else {
            throw new UnsupportedOperationException("Cannot launch a command of type " + resolvedCommand.type());
        }
    }

    @Nonnull
    private Container launchResolvedDockerCommand(final ResolvedCommand resolvedCommand,
                                                  final UserI userI)
            throws NoServerPrefException, DockerServerException, ContainerException {
        log.info("Preparing to launch resolved command.");
        final ResolvedCommand preparedToLaunch = prepareToLaunch(resolvedCommand, userI);

        log.info("Creating container from resolved command.");
        final String containerId = containerControlApi.createContainer(preparedToLaunch);

        log.info("Recording container launch.");
        final Container container = save(preparedToLaunch, containerId, userI);

        log.info("Starting container.");
        try {
            containerControlApi.startContainer(containerId);
        } catch (DockerServerException e) {
            addContainerHistoryItem(container, ContainerHistory.fromSystem("Failed", "Did not start." + e.getMessage()));
            handleFailure(container);
            throw new ContainerException("Failed to start");
        }

        return container;
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
    public void processEvent(final ContainerEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Processing container event");
        }
        final Container execution = addContainerEventToHistory(event);


        // execution will be null if either we aren't tracking the container
        // that this event is about, or if we have already recorded the event
        if (execution != null ) {

            final Matcher exitCodeMatcher =
                    exitCodePattern.matcher(event.getStatus());
            if (exitCodeMatcher.matches()) {
                log.debug("Container is dead. Finalizing.");
                final String exitCode = exitCodeMatcher.group(1);
                final String userLogin = execution.userId();
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
    public void finalize(final String containerId, final UserI userI) throws NotFoundException {
        finalize(get(containerId), userI);
    }

    @Override
    public void finalize(final Container container, final UserI userI) {
        String exitCode = "x";
        for (final ContainerHistory history : container.history()) {
            final Matcher exitCodeMatcher = exitCodePattern.matcher(history.status());
            if (exitCodeMatcher.matches()) {
                exitCode = exitCodeMatcher.group(1);
            }
        }
        finalize(container, userI, exitCode);
    }

    @Override
    public void finalize(final Container container, final UserI userI, final String exitCode) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Finalizing ContainerExecution %s for container %s", container.databaseId(), container.containerId()));
        }

        final Container finalized = containerFinalizeService.finalizeContainer(container, userI, exitCode);

        if (log.isInfoEnabled()) {
            log.info(String.format("Done uploading for ContainerExecution %s. Now saving information about created outputs.", container.databaseId()));
        }
        containerEntityService.update(fromPojo(finalized));
        if (log.isDebugEnabled()) {
            log.debug("Done saving outputs for Container " + String.valueOf(container.databaseId()));
        }
    }

    @Override
    @Nonnull
    public String kill(final String containerId, final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException {
        // TODO check user permissions. How?
        final Container container = get(containerId);

        addContainerHistoryItem(container, ContainerHistory.fromUserAction("Killed", userI.getLogin()));

        final String containerDockerId = container.containerId();
        containerControlApi.killContainer(containerDockerId);
        return containerDockerId;
    }

    private void handleFailure(final Container container) {
        // TODO handle failure
    }

    @Nonnull
    private Container toPojo(@Nonnull final ContainerEntity containerEntity) {
        return Container.create(containerEntity);
    }

    @Nonnull
    private List<Container> toPojo(@Nonnull final List<ContainerEntity> containerEntityList) {
        return Lists.newArrayList(Lists.transform(containerEntityList, new Function<ContainerEntity, Container>() {
            @Override
            public Container apply(final ContainerEntity input) {
                return toPojo(input);
            }
        }));
    }

    @Nonnull
    private ContainerHistory toPojo(@Nonnull final ContainerEntityHistory containerEntityHistory) {
        return ContainerHistory.create(containerEntityHistory);
    }

    @Nonnull
    private ContainerEntity fromPojo(@Nonnull final Container container) {
        final ContainerEntity template = containerEntityService.retrieve(container.databaseId());
        return template == null ? ContainerEntity.fromPojo(container) : template.update(container);
    }

    @Nonnull
    private ContainerEntityHistory fromPojo(@Nonnull final ContainerHistory containerHistory) {
        return ContainerEntityHistory.fromPojo(containerHistory);
    }
}
