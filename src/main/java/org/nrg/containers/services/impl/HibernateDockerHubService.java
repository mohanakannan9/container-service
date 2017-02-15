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
    public DockerHub getHub(final long hubId) throws NotFoundException {
        try {
            return get(hubId).toPojo();
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @Override
    public DockerHub getHub(final String hubName) {
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
        return getHub(containerConfigService.getDefaultDockerHubId());
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

    private boolean isDefault(final long id) {
        return containerConfigService.getDefaultDockerHubId() == id;
    }

    @Override
    public void delete(final DockerHub dockerHub) throws DockerHubDeleteException {
        delete(retrieve(dockerHub.id()));
    }

    @Override
    public void delete(final DockerHubEntity entity) throws DockerHubDeleteException {
        if (isDefault(entity.getId())) {
            throw new DockerHubDeleteException("Cannot delete default docker hub. Make another hub default first.");
        }
        super.delete(entity);
    }
}
