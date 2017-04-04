package org.nrg.containers.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.model.configuration.CommandConfigurationInternal;
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
    public static final String OPT_IN_PATH = "opt-in-to-site-commands";
    public static final boolean OPT_IN_DEFAULT_VALUE = false;
    public static final String ALL_ENABLED_PATH = "all-commands-enabled";

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
    public void configureForProject(final CommandConfigurationInternal commandConfigurationInternal, final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandConfigurationInternal(commandConfigurationInternal, Scope.Project, project, commandId, wrapperName, username, reason);
    }

    @Override
    public void configureForSite(final CommandConfigurationInternal commandConfigurationInternal, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandConfigurationInternal(commandConfigurationInternal, Scope.Site, null, commandId, wrapperName, username, reason);
    }

    @Override
    @Nullable
    public CommandConfigurationInternal getSiteConfiguration(final long commandId, final String wrapperName) {
        return getCommandConfiguration(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    @Nullable
    public CommandConfigurationInternal getProjectConfiguration(final String project, final long commandId, final String wrapperName) {
        final CommandConfigurationInternal siteConfig = getSiteConfiguration(commandId, wrapperName);
        final CommandConfigurationInternal projectConfig = getCommandConfiguration(Scope.Project, project, commandId, wrapperName);
        return siteConfig == null ? projectConfig : siteConfig.merge(projectConfig);
    }

    @Override
    public void deleteSiteConfiguration(final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        deleteCommandConfiguration(Scope.Site, null, commandId, wrapperName, username);
    }

    @Override
    public void deleteProjectConfiguration(final String project, final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        deleteCommandConfiguration(Scope.Project, project, commandId, wrapperName, username);
    }

    @Override
    public void deleteAllConfiguration(final long commandId, final String wrapperName) {
        // TODO
    }

    @Override
    public void deleteAllConfiguration(final long commandId) {
        // TODO
    }

    @Override
    public Boolean getOptInToSiteCommands() {
        final Boolean setting = parseBooleanConfig(configService.getConfig(TOOL_ID, OPT_IN_PATH, Scope.Site, null));
        return setting == null ? OPT_IN_DEFAULT_VALUE : setting;
    }

    @Override
    public void setOptInToSiteCommands(final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(true, username, reason);
    }

    @Override
    public void setOptOutOfSiteCommands(final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(false, username, reason);
    }

    @Override
    public void deleteOptInToSiteCommands(final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(OPT_IN_DEFAULT_VALUE, username, reason);
    }

    private void setOptInToSiteCommands(final boolean optInDefault, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_ENABLED_PATH,
                String.valueOf(optInDefault),
                Scope.Site, null);
    }

    @Override
    public Boolean getOptInToSiteCommands(final String project) {
        final Boolean projectSetting = parseBooleanConfig(configService.getConfig(TOOL_ID, OPT_IN_PATH, Scope.Project, project));
        return projectSetting == null ? getOptInToSiteCommands() : projectSetting;
    }

    @Override
    public void optInToSiteCommands(final String project, final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(true, project, username, reason);
    }

    @Override
    public void optOutOfSiteCommands(final String project, final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(false, project, username, reason);
    }

    @Override
    public void deleteOptInToSiteCommandsSetting(final String project, final String username, final String reason) throws ConfigServiceException {
        setOptInToSiteCommands(getOptInToSiteCommands(), project, username, reason);
    }

    private void setOptInToSiteCommands(final boolean optIn, final String project, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_ENABLED_PATH,
                String.valueOf(optIn),
                Scope.Project, project);
    }

    @Override
    @Nullable
    public Boolean getAllEnabled() {
        return parseBooleanConfig(configService.getConfig(TOOL_ID, ALL_ENABLED_PATH, Scope.Site, null));
    }

    @Override
    public void enableAll(final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(true, username, reason);
    }

    @Override
    public void disableAll(final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(false, username, reason);
    }

    @Override
    public void deleteAllEnabledSetting(final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(null, username, reason);
    }

    private void setAllEnabled(final Boolean allEnabled, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_ENABLED_PATH,
                allEnabled == null ? null : String.valueOf(allEnabled),
                Scope.Site, null);
    }

    @Override
    @Nullable
    public Boolean getAllEnabled(final String project) {
        return parseBooleanConfig(configService.getConfig(TOOL_ID, ALL_ENABLED_PATH, Scope.Project, project));
    }

    @Override
    public void enableAll(final String project, final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(true, project, username, reason);
    }

    @Override
    public void disableAll(final String project, final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(false, project, username, reason);
    }

    @Override
    public void deleteAllEnabledSetting(final String project, final String username, final String reason) throws ConfigServiceException {
        setAllEnabled(null, project, username, reason);
    }

    private void setAllEnabled(final Boolean allEnabled, final String project, final String username, final String reason) throws ConfigServiceException {
        configService.replaceConfig(username, reason,
                TOOL_ID, ALL_ENABLED_PATH,
                allEnabled == null ? null : String.valueOf(allEnabled),
                Scope.Project, project);
    }

    @Nullable
    private Boolean parseBooleanConfig(final @Nullable Configuration configuration) {
        return (configuration == null || configuration.getContents() == null) ? null : Boolean.parseBoolean(configuration.getContents());
    }

    @Override
    public void enableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(true, Scope.Site, null, commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForSite(final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(false, Scope.Site, null, commandId, wrapperName, username, reason);
    }

    @Override
    @Nullable
    public Boolean isEnabledForSite(final long commandId, final String wrapperName) {
        return getCommandIsEnabled(Scope.Site, null, commandId, wrapperName);
    }

    @Override
    public void enableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(true, Scope.Project, project, commandId, wrapperName, username, reason);
    }

    @Override
    public void disableForProject(final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        setCommandEnabled(false, Scope.Project, project, commandId, wrapperName, username, reason);
    }

    @Override
    @Nullable
    public Boolean isEnabledForProject(final String project, final long commandId, final String wrapperName) {
        return getCommandIsEnabled(Scope.Project, project, commandId, wrapperName);
    }

    private void setCommandEnabled(final Boolean enabled, final Scope scope, final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        final CommandConfigurationInternal alreadyExists = getCommandConfiguration(scope, project, commandId, wrapperName);
        final CommandConfigurationInternal toSet =
                (alreadyExists == null ? CommandConfigurationInternal.builder() : alreadyExists.toBuilder())
                        .enabled(enabled)
                        .build();
        setCommandConfigurationInternal(toSet, scope, project, commandId, wrapperName, username, reason);
    }

    private void setCommandConfigurationInternal(final CommandConfigurationInternal commandConfigurationInternal,
                                                 final Scope scope, final String project, final long commandId, final String wrapperName, final String username, final String reason) throws CommandConfigurationException {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        String contents = "";
        try {
            contents = mapper.writeValueAsString(commandConfigurationInternal);
        } catch (JsonProcessingException e) {
            final String message = String.format("Could not save configuration for command id %d, wrapper name \"%s\".", commandId, wrapperName);
            log.error(message);
            throw new CommandConfigurationException(message, e);
        }

        final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
        try {
            configService.replaceConfig(username, reason, TOOL_ID, path, contents, scope, project);
        } catch (ConfigServiceException e) {
            final String message = String.format("Could not save configuration for command id %d, wrapper name \"%s\".", commandId, wrapperName);
            log.error(message);
            throw new CommandConfigurationException(message, e);
        }
    }

    @Nullable
    private Boolean getCommandIsEnabled(final Scope scope, final String project, final long commandId, final String wrapperName) {
        final CommandConfigurationInternal commandConfigurationInternal = getCommandConfiguration(scope, project, commandId, wrapperName);
        return commandConfigurationInternal == null ? null : commandConfigurationInternal.enabled();
    }

    @Nullable
    private CommandConfigurationInternal getCommandConfiguration(final Scope scope, final String project, final long commandId, final String wrapperName) {
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
            return mapper.readValue(configurationJson, CommandConfigurationInternal.class);
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

    private void deleteCommandConfiguration(final Scope scope, final String project, final long commandId, final String wrapperName, final String username) throws CommandConfigurationException {
        if (scope.equals(Scope.Project) && StringUtils.isBlank(project)) {
            // TODO error: project can't be blank
        }

        if (commandId == 0L) {
            // TODO error
        }

        final CommandConfigurationInternal commandConfigurationInternal = getCommandConfiguration(scope, project, commandId, wrapperName);
        if (commandConfigurationInternal == null) {
            return;
        }
        if (commandConfigurationInternal.enabled() == null) {
            final String path = String.format(COMMAND_CONFIG_PATH_TEMPLATE, commandId, wrapperName != null ? wrapperName : "");
            configService.delete(configService.getConfig(TOOL_ID, path, scope, project));
            return;
        }

        setCommandConfigurationInternal(CommandConfigurationInternal.create(commandConfigurationInternal.enabled(), null),
                scope, project, commandId, wrapperName, username, "Deleting command configuration");
    }
}
