package org.nrg.containers.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.containers.services.ContainerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContainerConfigServiceImpl implements ContainerConfigService {
    private static final Logger log = LoggerFactory.getLogger(ContainerConfigService.class);
    private static final String TOOL_ID = "container-service";
    private static final String DEFAULT_DOCKER_HUB_PATH = "default-docker-hub-id";

    private final ConfigService configService;

    public ContainerConfigServiceImpl(final ConfigService configService) {
        this.configService = configService;
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
}
