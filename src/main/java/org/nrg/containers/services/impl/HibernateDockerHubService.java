package org.nrg.containers.services.impl;

import org.nrg.containers.daos.DockerHubDao;
import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.exceptions.NotUniqueException;
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
    public DockerHubEntity retrieve(final String name) throws NotUniqueException {
        return getDao().findByName(name);
    }

    @Override
    public DockerHubEntity get(final String name) throws NotUniqueException, NotFoundException {
        final DockerHubEntity dockerHubEntity = getDao().findByName(name);
        if (dockerHubEntity == null) {
            throw new NotFoundException("Could not find hub with name " + name);
        }
        return dockerHubEntity;
    }

    @Override
    public DockerHub retrieveHub(final long id) {
        return convert(retrieve(id));
    }

    @Override
    public DockerHub retrieveHub(final String name) throws NotUniqueException {
        return convert(retrieve(name));
    }

    @Override
    public DockerHub getHub(final long id) throws NotFoundException {
        try {
            return convert(get(id));
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            // TODO remove this when I convert to use the framework "get" - XNAT-4682
            throw new NotFoundException(e);
        }
    }

    @Override
    public DockerHub getHub(final String name) throws NotFoundException, NotUniqueException {
        return convert(get(name));
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
        DockerHubEntity dockerHubEntity = retrieve(dockerHub.id());
        if (dockerHubEntity == null) {
            dockerHubEntity = create(DockerHubEntity.fromPojo(dockerHub));
        }

        setDefault(dockerHubEntity, username, reason);
    }

    private void setDefault(final DockerHubEntity dockerHubEntity, final String username, final String reason) {
        containerConfigService.setDefaultDockerHubId(dockerHubEntity.getId(), username, reason);
    }

    @Override
    public void delete(final long id) throws DockerHubDeleteDefaultException {
        delete(retrieve(id));
    }

    @Override
    public void delete(final String name) throws DockerHubDeleteDefaultException, NotUniqueException {
        delete(retrieve(name));
    }

    @Override
    public void delete(final DockerHubEntity entity) throws DockerHubDeleteDefaultException {
        if (isDefault(entity.getId())) {
            throw new DockerHubDeleteDefaultException("Cannot delete default docker hub. Make another hub default first.");
        }
        super.delete(entity);
    }

    private DockerHub convert(final DockerHubEntity dockerHubEntity) {
        return dockerHubEntity == null ? null : dockerHubEntity.toPojo();
    }

    private boolean isDefault(final long id) {
        return containerConfigService.getDefaultDockerHubId() == id;
    }
}
