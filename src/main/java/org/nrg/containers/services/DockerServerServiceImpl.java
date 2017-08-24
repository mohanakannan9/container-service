package org.nrg.containers.services;

import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.model.server.docker.DockerServerEntity;
import org.nrg.framework.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Service
public class DockerServerServiceImpl implements DockerServerService {
    private final HibernateDockerServerEntityService dockerServerEntityService;

    @Autowired
    public DockerServerServiceImpl(final HibernateDockerServerEntityService dockerServerEntityService) {
        this.dockerServerEntityService = dockerServerEntityService;
    }

    @Override
    @Nullable
    public DockerServer retrieveServer() {
        return toPojo(dockerServerEntityService.getServer());
    }

    @Override
    @Nonnull
    public DockerServer getServer() throws NotFoundException {
        final DockerServer server = retrieveServer();
        if (server == null) {
            throw new NotFoundException("No container server defined.");
        }
        return server;
    }

    @Override
    public DockerServer setServer(final DockerServer dockerServer) {
        return toPojo(dockerServerEntityService.create(fromPojo(dockerServer)));
    }

    @Override
    public void update(final DockerServer dockerServer) {
        dockerServerEntityService.update(fromPojo(dockerServer));
    }

    @Nullable
    public DockerServer toPojo(final DockerServerEntity dockerServerEntity) {
        return dockerServerEntity == null ? null : DockerServer.create(dockerServerEntity);
    }

    @Nonnull
    public DockerServerEntity fromPojo(final DockerServer dockerServer) {
        final DockerServerEntity template = dockerServerEntityService.retrieve(dockerServer.id());
        return template == null ? DockerServerEntity.create(dockerServer) : template.update(dockerServer);
    }
}
