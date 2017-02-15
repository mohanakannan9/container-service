package org.nrg.containers.services;

import org.nrg.containers.exceptions.NotFoundException;

public interface ContainerConfigService {
    long getDefaultDockerHubId() throws NotFoundException;
    void setDefaultDockerHubId(long hubId, String username, String reason);
    void setDefaultDockerHubId(long hubId, String username);
}
