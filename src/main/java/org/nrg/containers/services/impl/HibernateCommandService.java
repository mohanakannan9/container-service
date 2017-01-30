package org.nrg.containers.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.api.ContainerControlApi;
import org.nrg.containers.daos.CommandDao;
import org.nrg.containers.exceptions.CommandResolutionException;
import org.nrg.containers.exceptions.DockerServerException;
import org.nrg.containers.exceptions.NoServerPrefException;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.helpers.CommandResolutionHelper;
import org.nrg.containers.model.Command;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.model.ResolvedDockerCommand;
import org.nrg.containers.model.XnatCommandWrapper;
import org.nrg.containers.services.CommandService;
import org.nrg.containers.services.ContainerExecutionService;
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
import java.util.Set;

@Service
@Transactional
public class HibernateCommandService extends AbstractHibernateEntityService<Command, CommandDao>
        implements CommandService {
    private static final Logger log = LoggerFactory.getLogger(HibernateCommandService.class);

    private ContainerControlApi controlApi;
    private AliasTokenService aliasTokenService;
    private SiteConfigPreferences siteConfigPreferences;
    private TransportService transporter;
    private ContainerExecutionService containerExecutionService;
    private ConfigService configService;

    @Autowired
    public HibernateCommandService(final ContainerControlApi controlApi,
                                   final AliasTokenService aliasTokenService,
                                   final SiteConfigPreferences siteConfigPreferences,
                                   final TransportService transporter,
                                   final ContainerExecutionService containerExecutionService,
                                   final ConfigService configService) {
        this.controlApi = controlApi;
        this.aliasTokenService = aliasTokenService;
        this.siteConfigPreferences = siteConfigPreferences;
        this.transporter = transporter;
        this.containerExecutionService = containerExecutionService;
        this.configService = configService;
    }

    @Override
    public void afterPropertiesSet() {
        // Set the default JayWay JSONPath configuration
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return Sets.newHashSet(Option.DEFAULT_PATH_LEAF_TO_NULL);
            }
        });
    }

    @Override
    public Command get(final Long id) throws NotFoundException {
        final Command command = retrieve(id);
        if (command == null) {
            throw new NotFoundException("Could not find Command with id " + id);
        }
        return command;
    }

    @Override
    public Command update(final Long id, final Command updated, final Boolean ignoreNull)
            throws NotFoundException {
        // final Command existing = get(id);
        // existing.update(updated, ignoreNull);
        //
        // super.update(existing);
        // return existing;
        // TODO re-write this to save a new version of the command
        return null;
    }

    @Override
    public void initialize(final Command command) {
        if (command == null) {
            return;
        }
        Hibernate.initialize(command);
        Hibernate.initialize(command.getEnvironmentVariables());
        Hibernate.initialize(command.getMounts());
        Hibernate.initialize(command.getInputs());
        Hibernate.initialize(command.getOutputs());
        Hibernate.initialize(command.getXnatCommandWrappers());
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
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        final Command command = get(commandId);
        return resolveCommand(command, runtimeInputValues, userI);
    }

    @Override
    public ResolvedCommand resolveCommand(final Command command,
                                          final Map<String, String> runtimeInputValues,
                                          final UserI userI)
            throws NotFoundException, CommandResolutionException {
        return CommandResolutionHelper.resolve(XnatCommandWrapper.passthrough(command), command, runtimeInputValues, userI, configService);
    }

    @Override
    public ContainerExecution resolveAndLaunchCommand(final Long commandId,
                                                      final Map<String, String> runtimeValues,
                                                      final UserI userI)
            throws NoServerPrefException, DockerServerException, NotFoundException, CommandResolutionException {
        final ResolvedCommand resolvedCommand = resolveCommand(commandId, runtimeValues, userI);
        switch (resolvedCommand.getType()) {
            case DOCKER:
                return launchResolvedDockerCommand((ResolvedDockerCommand) resolvedCommand, userI);
            default:
                return null; // TODO throw error
        }
    }

    @Override
    public ContainerExecution launchResolvedDockerCommand(final ResolvedDockerCommand resolvedDockerCommand,
                                                          final UserI userI)
            throws NoServerPrefException, DockerServerException {
        log.info("Preparing to launch resolved command.");
        final ResolvedDockerCommand preparedToLaunch = prepareToLaunch(resolvedDockerCommand, userI);

        log.info("Launching resolved command.");
        final String containerId = controlApi.launchImage(preparedToLaunch);

        log.info("Recording command launch.");
        return containerExecutionService.save(preparedToLaunch, containerId, userI);
    }

    private ResolvedDockerCommand prepareToLaunch(final ResolvedDockerCommand resolvedDockerCommand,
                                                  final UserI userI)
            throws NoServerPrefException {
        // Add default environment variables
        final Map<String, String> defaultEnv = Maps.newHashMap();
//        siteConfigPreferences.getBuildPath()
        defaultEnv.put("XNAT_HOST", (String)siteConfigPreferences.getProperty("processingUrl", siteConfigPreferences.getSiteUrl()));

        final AliasToken token = aliasTokenService.issueTokenForUser(userI);
        defaultEnv.put("XNAT_USER", token.getAlias());
        defaultEnv.put("XNAT_PASS", token.getSecret());

        if (log.isDebugEnabled()) {
            log.debug("Adding default environment variables");
            for (final String envKey : defaultEnv.keySet()) {
                log.debug(String.format("%s=%s", envKey, defaultEnv.get(envKey)));
            }
        }
        resolvedDockerCommand.addEnvironmentVariables(defaultEnv);

        // Transport mounts
        if (log.isDebugEnabled() && ((resolvedDockerCommand.getMountsIn() != null && !resolvedDockerCommand.getMountsIn().isEmpty()) ||
                (resolvedDockerCommand.getMountsOut() != null && !resolvedDockerCommand.getMountsOut().isEmpty()))) {
            log.debug("Transporting mounts");
        }
        if (resolvedDockerCommand.getMountsIn() != null && !resolvedDockerCommand.getMountsIn().isEmpty()) {
            final String dockerHost = controlApi.getServer().getHost();
            for (final ContainerExecutionMount mountIn : resolvedDockerCommand.getMountsIn()) {
                final Path pathOnXnatHost = Paths.get(mountIn.getHostPath());
                final Path pathOnDockerHost = transporter.transport(dockerHost, pathOnXnatHost);
                mountIn.setHostPath(pathOnDockerHost.toString());
            }
        }
        if (resolvedDockerCommand.getMountsOut() != null && !resolvedDockerCommand.getMountsOut().isEmpty()) {
            final String dockerHost = controlApi.getServer().getHost();
            final List<ContainerExecutionMount> mountsOut = resolvedDockerCommand.getMountsOut();
            final List<Path> buildPaths = transporter.getWritableDirectories(dockerHost, mountsOut.size());
            for (int i=0; i < mountsOut.size(); i++) {
                final ContainerExecutionMount mountOut = mountsOut.get(i);
                final Path buildPath = buildPaths.get(i);

                mountOut.setHostPath(buildPath.toString());
            }
        }

        return resolvedDockerCommand;
    }
}
