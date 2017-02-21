package org.nrg.containers.services.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;

@Service
@Transactional
public class HibernateDockerHubService
        extends AbstractHibernateEntityService<DockerHubEntity, DockerHubDao>
        implements DockerHubService {
    private static final Logger log = LoggerFactory.getLogger(HibernateDockerHubService.class);

    private final ContainerConfigService containerConfigService;

    @Autowired
    public HibernateDockerHubService(final ContainerConfigService containerConfigService) {
        this.containerConfigService = containerConfigService;
    }

    @Override
    public DockerHubEntity retrieve(final String name) throws NotUniqueException {
        final DockerHubEntity dockerHubEntity = getDao().findByName(name);
        if (dockerHubEntity == null) {
            try {
                // We didn't find it by name. If the name is a number, maybe they meant to find it by ID?
                final Long id = Long.valueOf(name);

                // If we got to this line, then the name is a proper ID and we should give them that hub.
                return retrieve(id);
            } catch (NumberFormatException ignored) {
                // Nope, name is not an id
            }
        }

        // If we got here, we should return this thing, null or not
        return dockerHubEntity;
    }

    @Override
    public DockerHubEntity get(final String name) throws NotUniqueException, NotFoundException {
        final DockerHubEntity dockerHubEntity = retrieve(name);
        if (dockerHubEntity == null) {
            throw new NotFoundException("Could not find hub with name " + name);
        }
        return dockerHubEntity;
    }

    @Override
    public DockerHub retrieveHub(final long id) {
        return toPojo(retrieve(id));
    }

    @Override
    public DockerHub retrieveHub(final String name) throws NotUniqueException {
        return toPojo(retrieve(name));
    }

    @Override
    public DockerHub getHub(final long id) throws NotFoundException {
        try {
            return toPojo(get(id));
        } catch (org.nrg.framework.exceptions.NotFoundException e) {
            // TODO remove this when I toPojo to use the framework "get" - XNAT-4682
            throw new NotFoundException(e);
        }
    }

    @Override
    public DockerHub getHub(final String name) throws NotFoundException, NotUniqueException {
        return toPojo(get(name));
    }

    @Override
    public List<DockerHub> getHubs() {
        return toPojo(getAll());
    }

    @Override
    public DockerHub create(final DockerHub dockerHub) {
        return toPojo(create(fromPojo(dockerHub)));
    }

    @Override
    public DockerHubEntity createAndSetDefault(final DockerHubEntity dockerHubEntity, final String username, final String reason) {
        final DockerHubEntity created = create(dockerHubEntity);
        setDefault(created, username, reason);
        return created;
    }

    @Override
    public DockerHub createAndSetDefault(final DockerHub dockerHub, final String username, final String reason) {
        return toPojo(createAndSetDefault(fromPojo(dockerHub), username, reason));
    }

    @Override
    public void update(final DockerHub dockerHub) {
        update(fromPojo(dockerHub));
    }

    @Override
    public void updateAndSetDefault(final DockerHubEntity dockerHubEntity, final String username, final String reason) {
        update(dockerHubEntity);
        setDefault(dockerHubEntity, username, reason);
    }

    @Override
    public void updateAndSetDefault(final DockerHub dockerHub, final String username, final String reason) {
        updateAndSetDefault(fromPojo(dockerHub), username, reason);
    }

    @Override
    public DockerHub getDefault() throws NotFoundException {
        return getHub(containerConfigService.getDefaultDockerHubId());
    }

    @Override
    public void setDefault(final DockerHub dockerHub, final String username, final String reason) {
        setDefault(fromPojo(dockerHub), username, reason);
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

    private DockerHub toPojo(final DockerHubEntity dockerHubEntity) {
        return dockerHubEntity == null ? null : dockerHubEntity.toPojo();
    }

    private List<DockerHub> toPojo(final List<DockerHubEntity> dockerHubEntityList) {
        return dockerHubEntityList == null ? null :
                Lists.transform(dockerHubEntityList, new Function<DockerHubEntity, DockerHub>() {
                    @Nullable
                    @Override
                    public DockerHub apply(@Nullable final DockerHubEntity dockerHubEntity) {
                        return toPojo(dockerHubEntity);
                    }
                });
    }

    private DockerHubEntity fromPojo(final DockerHub dockerHub) {
        return dockerHub == null ? null : DockerHubEntity.fromPojo(dockerHub);
    }

    private boolean isDefault(final long id) {
        return containerConfigService.getDefaultDockerHubId() == id;
    }
}
