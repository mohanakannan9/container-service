package org.nrg.containers.daos;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.nrg.containers.events.DockerContainerEvent;
import org.nrg.containers.model.ContainerExecution;
import org.nrg.containers.model.ContainerExecutionHistory;
import org.nrg.containers.model.ContainerExecutionMount;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ContainerExecutionRepository extends AbstractHibernateDAO<ContainerExecution> {
    private static final Logger log = LoggerFactory.getLogger(ContainerExecutionRepository.class);

    @Override
    public void initialize(final ContainerExecution entity) {
        if (entity == null) {
            return;
        }
        Hibernate.initialize(entity);
        Hibernate.initialize(entity.getEnvironmentVariables());
        Hibernate.initialize(entity.getHistory());
        Hibernate.initialize(entity.getMounts());
        if (entity.getMounts() != null) {
            for (final ContainerExecutionMount mount : entity.getMounts()) {
                Hibernate.initialize(mount.getInputFiles());
            }
        }
        Hibernate.initialize(entity.getCommandLine());
        Hibernate.initialize(entity.getRawInputValues());
        Hibernate.initialize(entity.getXnatInputValues());
        Hibernate.initialize(entity.getCommandInputValues());
        Hibernate.initialize(entity.getOutputs());
        Hibernate.initialize(entity.getLogPaths());
    }

    public ContainerExecution didRecordEvent(final DockerContainerEvent event) {

        final List<ContainerExecution> matchingContainerIds = findByProperty("containerId", event.getContainerId());

        // Container ID is constrained to be unique, so we can safely take the first element of this list
        if (matchingContainerIds != null && !matchingContainerIds.isEmpty()) {
            final ContainerExecution execution = matchingContainerIds.get(0);
            if (log.isDebugEnabled()) {
                log.debug("Found matching execution: " + execution.getId());
            }

            String status = event.getStatus();
            if (status.matches("kill|die|oom")) {
                final Map<String, String> attributes = event.getAttributes();
                final String exitCode =
                        attributes.containsKey("exitCode") &&
                                StringUtils.isNotBlank(attributes.get("exitCode")) ?
                                attributes.get("exitCode") :
                                "x";

                status += "(" + exitCode + ")";
            }
            final ContainerExecutionHistory newHistory =
                    new ContainerExecutionHistory(status, event.getTimeNano());

            final List<ContainerExecutionHistory> historyList = execution.getHistory();
            if (historyList != null && !historyList.isEmpty()) {
                for (final ContainerExecutionHistory history : historyList) {
                    if (history.getTimeNano() != null && history.getTimeNano().equals(newHistory.getTimeNano())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Event has already been recorded in the history.");
                        }
                        return null;
                    }
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Adding history entry: " + newHistory);
            }
            execution.addToHistory(newHistory);
            update(execution);

            return execution;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No execution found for container " + event.getContainerId());
            }
        }

        return null;
    }
}
