package org.nrg.containers.services;

import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.containers.model.configuration.CommandConfigurationInternal;
import org.nrg.containers.model.settings.ContainerServiceSettings;

public interface ContainerConfigService {
    String TOOL_ID = "container-service";
    String DEFAULT_DOCKER_HUB_PATH = "default-docker-hub-id";
    String WRAPPER_CONFIG_PATH_TEMPLATE = "wrapper-%d";
    String OPT_IN_PATH = "opt-in-to-site-commands";
    boolean OPT_IN_DEFAULT_VALUE = false;

    long getDefaultDockerHubId();
    void setDefaultDockerHubId(long hubId, String username, String reason);

    void configureForSite(CommandConfigurationInternal commandConfiguration, long wrapperId, String username, String reason) throws CommandConfigurationException;
    void configureForProject(CommandConfigurationInternal commandConfiguration, String project, long wrapperId, String username, String reason) throws CommandConfigurationException;

    CommandConfigurationInternal getSiteConfiguration(long wrapperId);
    CommandConfigurationInternal getProjectConfiguration(String project, long wrapperId);

    void deleteSiteConfiguration(long commandId, final String username) throws CommandConfigurationException;
    void deleteProjectConfiguration(String project, long wrapperId, final String username) throws CommandConfigurationException;
    void deleteAllConfiguration(long wrapperId);

    void enableForSite(long wrapperId, final String username, final String reason) throws CommandConfigurationException;

    void disableForSite(long wrapperId, final String username, final String reason) throws CommandConfigurationException;
    boolean isEnabledForSite(long wrapperId);
    void enableForProject(String project, long wrapperId, final String username, final String reason) throws CommandConfigurationException;
    void disableForProject(String project, long wrapperId, final String username, final String reason) throws CommandConfigurationException;
    boolean isEnabledForProject(String project, long wrapperId);

    ContainerServiceSettings getSettings();
    ContainerServiceSettings getSettings(String project);

    Boolean getOptInToSiteCommands();
    void setOptInToSiteCommands(String username, String reason) throws ConfigServiceException;
    void setOptOutOfSiteCommands(String username, String reason) throws ConfigServiceException;
    void deleteOptInToSiteCommands(String username, String reason) throws ConfigServiceException;
    Boolean getOptInToSiteCommands(String project);
    void optInToSiteCommands(String project, String username, String reason) throws ConfigServiceException;
    void optOutOfSiteCommands(String project, String username, String reason) throws ConfigServiceException;
    void deleteOptInToSiteCommandsSetting(String project, String username, String reason) throws ConfigServiceException;

    class CommandConfigurationException extends Exception {
        public CommandConfigurationException(final String message, final Throwable e) {
            super(message, e);
        }
    }
}
