package org.nrg.execution.services;

import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

public interface ContainerExecutionService extends BaseHibernateService<ContainerExecution> {
    void processEvent(final DockerContainerEvent event);
    void finalize(final ContainerExecution execution);
    ContainerExecution save(final ResolvedCommand resolvedCommand, final String containerId, final UserI userI);
}
