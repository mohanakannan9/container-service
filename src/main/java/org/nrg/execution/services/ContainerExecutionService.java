package org.nrg.execution.services;

import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface ContainerExecutionService extends BaseHibernateService<ContainerExecution> {
    void finalize(final ContainerExecution execution);

    ContainerExecution save(final ResolvedCommand resolvedCommand, final String containerId);
}
