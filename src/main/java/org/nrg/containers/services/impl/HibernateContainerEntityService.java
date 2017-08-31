package org.nrg.containers.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.daos.ContainerEntityRepository;
import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.command.auto.ResolvedCommand;
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
        final ContainerEntity createdContainer = new ContainerEntity(resolvedCommand, containerId, userI.getLogin());
        log.debug("Creating ContainerEntity for container with id " + containerId);
        final ContainerEntity created = create(createdContainer);
        final ContainerEntityHistory historyItem = ContainerEntityHistory.fromUserAction("Created", userI.getLogin(), created);
        addContainerHistoryItem(created, historyItem);
        return created;
    }

    @Override
    @Nullable
    public ContainerEntity retrieve(final String containerId) {
        if (StringUtils.isBlank(containerId)) {
            return null;
        }
        try {
            // This will allow the higher-level API to request the container by database id or docker hash id
            final Long containerDatabaseId = Long.parseLong(containerId);
            return retrieve(containerDatabaseId);
        } catch (NumberFormatException e) {
            final ContainerEntity containerEntity = getDao().retrieveByContainerId(containerId);
            initialize(containerEntity);
            return containerEntity;
        }
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
    public void delete(final String containerId) throws NotFoundException {
        final ContainerEntity toDelete = get(containerId);
        delete(toDelete.getId());
    }

    @Override
    @Nullable
    public ContainerEntity addContainerEventToHistory(final ContainerEvent containerEvent) {
        final ContainerEntity containerEntity = retrieve(containerEvent.containerId());
        if (containerEntity == null) {
            if (log.isDebugEnabled()) {
                log.debug("This event is not about a container we are interested in.");
            }
            return null;
        }

        final ContainerEntityHistory added = addContainerHistoryItem(containerEntity, ContainerEntityHistory.fromContainerEvent(containerEvent, containerEntity));
        return added == null ? null : containerEntity; // Return null if we've already added the history item
    }

    @Override
    @Nullable
    public ContainerEntityHistory addContainerHistoryItem(final ContainerEntity containerEntity,
                                                          final ContainerEntityHistory history) {
        if (containerEntity.isItemInHistory(history)) {
            if (log.isDebugEnabled()) {
                log.debug("Event has already been recorded.");
            }
            return null;
        }

        log.info("Adding new history item to container entity " + containerEntity.getId() + " from event.");
        if (log.isDebugEnabled()) {
            log.debug("" + history);
        }
        getDao().addHistoryItem(containerEntity, history);
        return history;
    }
}
