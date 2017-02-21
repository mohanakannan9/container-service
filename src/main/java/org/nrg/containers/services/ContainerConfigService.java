package org.nrg.containers.services;

public interface ContainerConfigService {
    long getDefaultDockerHubId();
    void setDefaultDockerHubId(long hubId, String username, String reason);
}
