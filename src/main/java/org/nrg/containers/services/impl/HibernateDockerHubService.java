package org.nrg.containers.services.impl;

import org.nrg.containers.daos.DockerHubDao;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.containers.services.ContainerConfigService;
import org.nrg.containers.services.DockerHubService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDockerHubService
        extends AbstractHibernateEntityService<DockerHubEntity, DockerHubDao>
        implements DockerHubService {
    private static final Logger log = LoggerFactory.getLogger(HibernateDockerHubService.class);

    private final ContainerConfigService containerConfigService;

    public HibernateDockerHubService(final ContainerConfigService containerConfigService) {
        this.containerConfigService = containerConfigService;
    }

    @Override
    public DockerHub get(final long hubId) throws NotFoundException {
        return getEntity(hubId).toPojo();
    }

    private DockerHubEntity getEntity(final long hubId) throws NotFoundException {
        final DockerHubEntity dockerHubEntity = retrieve(hubId);
        if (dockerHubEntity == null) {
            throw new NotFoundException(String.format("Hub with id %d not found", hubId));
        }
        return dockerHubEntity;
    }

    @Override
    public DockerHub get(final String hubName) {
        final DockerHubEntity dockerHubEntity = getDao().findByName(hubName);
        return dockerHubEntity == null ? null : dockerHubEntity.toPojo();
    }

    @Override
    public DockerHub create(final DockerHub dockerHub) {
        final DockerHubEntity created = create(DockerHubEntity.fromPojo(dockerHub));
        if (created == null) {
            return null;
        }
        return created.toPojo();
    }

    @Override
    public DockerHub getDefault() throws NotFoundException {
        return get(containerConfigService.getDefaultDockerHubId());
    }

    @Override
    public void setDefault(final DockerHub dockerHub, final String username, final String reason) {
        DockerHubEntity dockerHubEntity = getDao().findByName(dockerHub.name());
        if (dockerHubEntity == null) {
            dockerHubEntity = create(DockerHubEntity.fromPojo(dockerHub));
        }

        setDefault(dockerHubEntity, username, reason);
    }

    private void setDefault(final DockerHubEntity dockerHubEntity, final String username, final String reason) {
        containerConfigService.setDefaultDockerHubId(dockerHubEntity.getId(), username, reason);
    }
}
