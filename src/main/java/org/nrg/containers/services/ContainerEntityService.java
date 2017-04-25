package org.nrg.containers.services;

import org.nrg.containers.events.model.ContainerEvent;
import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.containers.model.container.entity.ContainerEntityHistory;
import org.nrg.containers.model.command.auto.ResolvedCommand;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

public interface ContainerEntityService extends BaseHibernateService<ContainerEntity> {
    ContainerEntity save(final ResolvedCommand resolvedCommand,
                         final String containerId,
                         final UserI userI);

    ContainerEntity retrieve(final String containerId);
    ContainerEntity get(final String containerId) throws NotFoundException;

    ContainerEntity addContainerEventToHistory(final ContainerEvent containerEvent);
    ContainerEntityHistory addContainerHistoryItem(final ContainerEntity containerEntity,
                                                   final ContainerEntityHistory history);
}
