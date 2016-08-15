package org.nrg.execution.daos;

import org.hibernate.criterion.Restrictions;
import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ContainerExecutionHistory;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ContainerExecutionRepository extends AbstractHibernateDAO<ContainerExecution> {
    @Transactional
    public ContainerExecution addEventToHistory(final DockerContainerEvent event) {
        final List<ContainerExecution> matchingContainerIds = findByCriteria(Restrictions.eq("containerId", event.getContainerId()));

        // Container ID is constrained to be unique, so we can safely take the first element of this list
        if (matchingContainerIds != null && !matchingContainerIds.isEmpty()) {
            final ContainerExecution execution = matchingContainerIds.get(0);
            execution.addToHistory(new ContainerExecutionHistory(event.getStatus(), event.getTime()));
            update(execution);
            return execution;
        }

        return null;
    }
}
