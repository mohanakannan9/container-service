package org.nrg.containers.services;

import org.nrg.containers.model.ContainerEntity;
import org.nrg.containers.model.ResolvedCommand;
import org.nrg.framework.exceptions.NotFoundException;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

public interface ContainerEntityService extends BaseHibernateService<ContainerEntity> {
    ContainerEntity save(final ResolvedCommand resolvedCommand,
                         final String containerId,
                         final UserI userI);

    ContainerEntity retrieve(final String containerId);
    ContainerEntity get(final String containerId) throws NotFoundException;

    ContainerEntity addContainerEvent(final String containerId,
                                      final String status,
                                      final long time);
    void addContainerEvent(final ContainerEntity containerEntity,
                           final String status,
                           final long time);
}
