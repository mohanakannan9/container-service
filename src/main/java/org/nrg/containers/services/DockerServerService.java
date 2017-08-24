package org.nrg.containers.services;

import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.framework.exceptions.NotFoundException;

public interface DockerServerService {
    DockerServer retrieveServer();
    DockerServer getServer() throws NotFoundException;
    DockerServer setServer(DockerServer dockerServer);
    void update(DockerServer dockerServer);
}
