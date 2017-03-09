package org.nrg.containers.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.daos.ContainerEntityRepository;
import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.model.ContainerEntityHistory;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.containers.services.ContainerEntityService;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Service
@Transactional
public class HibernateContainerEntityService
        extends AbstractHibernateEntityService<ContainerEntity, ContainerEntityRepository>
        implements ContainerEntityService {
    private static final Logger log = LoggerFactory.getLogger(HibernateContainerEntityService.class);

    @Override
    @Nonnull
    public ContainerEntity save(final ResolvedCommand resolvedCommand,
                                final String containerId,
                                final UserI userI) {
        final ContainerEntity execution = new ContainerEntity(resolvedCommand, containerId, userI.getLogin());
        return create(execution);
    }

    @Override
    @Nullable
    public ContainerEntity retrieve(final String containerId) {
        if (StringUtils.isBlank(containerId)) {
            return null;
        }
        return getDao().retrieveByContainerId(containerId);
    }

    @Override
    @Nonnull
    public ContainerEntity get(final String containerId) throws NotFoundException {
        final ContainerEntity containerEntity = retrieve(containerId);
        if (containerEntity == null) {
            throw new NotFoundException("No container with ID " + containerId);
        }
        return containerEntity;
    }

    @Override
    @Nullable
    public ContainerEntity addContainerEvent(final String containerId,
                                  final String status,
                                  final long time) {
        final ContainerEntity containerEntity = retrieve(containerId);
        if (containerEntity == null) {
            if (log.isDebugEnabled()) {
                log.debug("This event is not about a container we are interested in.");
            }
            return null;
        }
        addContainerEvent(containerEntity, status, time);
        return containerEntity;
    }

    public void addContainerEvent(final ContainerEntity containerEntity,
                                  final String status,
                                  final long time) {
        if (getDao().eventHasBeenRecorded(containerEntity.getContainerId(), status, time)) {
            if (log.isDebugEnabled()) {
                log.debug("Event has already been recorded in the history.");
            }
            return;
        }

        final ContainerEntityHistory newHistory =
                new ContainerEntityHistory(status, time);


        if (log.isDebugEnabled()) {
            log.debug("Adding history entry: " + newHistory);
        }
        containerEntity.addToHistory(newHistory);
        getDao().persistEvent(newHistory);
        update(containerEntity);
    }
}
