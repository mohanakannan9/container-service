package org.nrg.execution.services;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.daos.ContainerExecutionRepository;
import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ContainerExecutionHistory;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HibernateContainerExecutionService
        extends AbstractHibernateEntityService<ContainerExecution, ContainerExecutionRepository>
        implements ContainerExecutionService {
    private static final Logger log = LoggerFactory.getLogger(HibernateContainerExecutionService.class);

    @Override
    @Transactional
    public void processEvent(final DockerContainerEvent event) {
        log.info("Processing docker container event: " + event);
//        final List<ContainerExecution> matchingContainerIds = getDao().findByProperty("containerId", event.getContainerId());
        final List<ContainerExecution> matchingContainerIds = Lists.newArrayList();
        final List<ContainerExecution> all = getAll();
        if (all == null || all.isEmpty()) {
            log.error("I FOUND NO EXECUTIONS matching event " + event);
            return;
        }
        log.info("I found at least one container execution: " + all);
        for (final ContainerExecution candidate : all) {
            if (candidate.getContainerId().equals(event.getContainerId())) {
                matchingContainerIds.add(candidate);
            }
        }

        // Container ID is constrained to be unique, so we can safely take the first element of this list
        if (matchingContainerIds != null && !matchingContainerIds.isEmpty()) {
            final ContainerExecution execution = matchingContainerIds.get(0);
            log.info("Found matching execution: " + execution);

            final ContainerExecutionHistory history = new ContainerExecutionHistory(event.getStatus(), event.getTime());
            log.info("Adding history entry: "+history);
            execution.addToHistory(history);
            update(execution);

            if (StringUtils.isNotBlank(event.getStatus()) &&
                    event.getStatus().matches("kill|die|oom")) {
                finalize(execution);
            }
        }
    }

    @Override
    @Transactional
    public void finalize(final ContainerExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Finalizing ContainerExecution for container %s, status %s", execution.getContainerId(), execution.getHistory().get(execution.getHistory().size()-1).getStatus());
        }
        // TODO upload logs

        // TODO upload output files
    }

    @Override
    @Transactional
    public ContainerExecution save(final ResolvedCommand resolvedCommand, final String containerId, final UserI userI) {
        final ContainerExecution execution = new ContainerExecution(resolvedCommand, containerId, userI.getLogin());
        return create(execution);
    }
}
