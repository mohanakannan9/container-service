package org.nrg.containers.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.model.CommandConfiguration;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.framework.constants.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.IOException;

@Service
public class ContainerConfigServiceImpl implements ContainerConfigService {
    private static final Logger log = LoggerFactory.getLogger(ContainerConfigService.class);

    public static final String DEFAULT_DOCKER_HUB_PATH = "default-docker-hub-id";
    public static final String COMMAND_CONFIG_PATH_TEMPLATE = "command-%d-wrapper-%s";

    private final ConfigService configService;
    private final ObjectMapper mapper;

    @Autowired
    public ContainerConfigServiceImpl(final ConfigService configService,
                                      final ObjectMapper mapper) {
        this.configService = configService;
        this.mapper = mapper;
    }

    @Override
    public long getDefaultDockerHubId() {
        final Configuration defaultDockerHubConfig = configService.getConfig(TOOL_ID, DEFAULT_DOCKER_HUB_PATH);
        long id = 0L;
        if (defaultDockerHubConfig != null) {
            final String contents = defaultDockerHubConfig.getContents();
            if (StringUtils.isNotBlank(contents)) {
                try {
                    id = Long.valueOf(contents);
                } catch (NumberFormatException ignored) {
                    // ignored
                }
            }
        }
        return id;
    }

    @Override
    public void setDefaultDockerHubId(final long hubId, final String username, final String reason) {
        try {
            configService.replaceConfig(username, reason, TOOL_ID, DEFAULT_DOCKER_HUB_PATH, String.valueOf(hubId));
        } catch (ConfigServiceException e) {
            log.error("Could not save default docker hub config.", e);
        }
    }

    @Override
    public void configureForProject(final String project, final long commandId, final String wrapperName, final CommandConfiguration commandConfiguration, final String username, final String reason) throws CommandConfigurationException {
        configure(Scope.Project, project, commandId, wrapperName, commandConfiguration, username, reason);
    }

    @Override
    public void configureForSite(final long commandId, final String wrapperName, final CommandConfiguration commandConfiguration, final String username, final String reason) throws CommandConfigurationException {
        configure(Scope.Site, null, commandId, wrapperName, commandConfiguration, username, reason);
    }

    @Override
    @Nullable
    public CommandConfiguration getSiteConfiguration(final long commandId, final String wrapperName) {
        return get(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    @Nullable
    public CommandConfiguration getProjectConfiguration(final String project, final long commandId, final String wrapperName) {
        final CommandConfiguration siteConfig = getSiteConfiguration(commandId, wrapperName);
        final CommandConfiguration projectConfig = get(Scope.Project, project, commandId, wrapperName);
        return siteConfig == null ? projectConfig : siteConfig.merge(projectConfig);
    }

    @Override
    public void deleteSiteConfiguration(final long commandId, final String wrapperName) throws CommandConfigurationException {
        delete(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    public void deleteProjectConfiguration(final String project, final long commandId, final String wrapperName) throws CommandConfigurationException {
        delete(Scope.Project, project, commandId, wrapperName);
    }

    @Override
    public void deleteAllConfiguration(final long commandId, final String wrapperName) {
        // TODO
    }

    @Override
    public void deleteAllConfiguration(final long commandId) {
        // TODO
    }

    private void configure(final Scope scope, final String project, final long commandId, final String wrapperName, final CommandConfiguration commandConfiguration, final String username, final String reason) throws CommandConfigurationException{
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        if (commandConfiguration == null) {
            // TODO is this an error? Or should we delete the configuration?
        }

        String contents = "";
        try {
            contents = mapper.writeValueAsString(commandConfiguration);
        } catch (JsonProcessingException e) {
            // TODO handle this
            e.printStackTrace();
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        try {
            configService.replaceConfig(username, reason, TOOL_ID, path, contents, scope, project);
        } catch (ConfigServiceException e) {
            log.error("Could not save configuration for command id {}, wrapper name \"{}\".", commandId, wrapperName);
            throw new CommandConfigurationException(String.format("Could not save configuration for command id %d, wrapper name \"%s\".", commandId, wrapperName), e);
        }
    }

    @Nullable
    private CommandConfiguration get(final Scope scope, final String project, final long commandId, final String wrapperName) {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        final Configuration configuration = configService.getConfig(TOOL_ID, path, scope, project);
        if (configuration == null) {
            return null;
        }

        final String configurationJson = configuration.getContents();
        if (StringUtils.isBlank(configurationJson)) {
            return null;
        }

        try {
            return mapper.readValue(configurationJson, CommandConfiguration.class);
        } catch (IOException e) {
            final String message = String.format("Could not deserialize Command Configuration for %s, command id %s, wrapper \"%s\".",
                    scope.equals(Scope.Site) ? "site" : "project " + project,
                    commandId,
                    wrapperName);
            log.error(message, e);
            //throw new CommandConfigurationException(message, e);
        }
        return null;
    }

    private void delete(final Scope scope, final String project, final long commandId, final String wrapperName) throws CommandConfigurationException {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        final Configuration configuration = configService.getConfig(TOOL_ID, path, scope, project);
        if (configuration == null) {
            return;
        }
        configService.delete(configuration);
    }
}
