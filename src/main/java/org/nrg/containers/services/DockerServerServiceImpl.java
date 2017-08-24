package org.nrg.containers.services;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.nrg.containers.model.server.docker.DockerServerBase;
import org.nrg.containers.model.server.docker.DockerServerBase.DockerServer;
import org.nrg.containers.model.server.docker.DockerServerEntity;
import org.nrg.framework.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Service
public class DockerServerServiceImpl implements DockerServerService {
    private final HibernateDockerServerEntityService dockerServerEntityService;

    @Autowired
    public DockerServerServiceImpl(final HibernateDockerServerEntityService dockerServerEntityService) {
        this.dockerServerEntityService = dockerServerEntityService;
    }

    @Override
    @Nonnull
    public List<DockerServer> getServers() {
        return toPojo(dockerServerEntityService.getAllWithDisabled());
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
    public List<DockerServer> toPojo(final List<DockerServerEntity> dockerServerEntities) {
        final List<DockerServer> returnList = Lists.newArrayList();
        if (dockerServerEntities != null) {
            returnList.addAll(Lists.transform(dockerServerEntities, new Function<DockerServerEntity, DockerServer>() {
                @Override
                public DockerServer apply(final DockerServerEntity input) {
                    return toPojo(input);
                }
            }));
        }
        return returnList;
    }

    @Nonnull
    public DockerServerEntity fromPojo(final DockerServer dockerServer) {
        final DockerServerEntity template = dockerServerEntityService.retrieve(dockerServer.id());
        return template == null ? DockerServerEntity.create(dockerServer) : template.update(dockerServer);
    }
}
