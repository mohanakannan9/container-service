package org.nrg.execution.services;

import org.apache.commons.lang3.StringUtils;
import org.nrg.execution.daos.ContainerExecutionRepository;
import org.nrg.execution.events.DockerContainerEvent;
import org.nrg.execution.model.ContainerExecution;
import org.nrg.execution.model.ResolvedCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.bus.Event;
import reactor.fn.Consumer;

@Service
public class HibernateContainerExecutionService
        extends AbstractHibernateEntityService<ContainerExecution, ContainerExecutionRepository>
        implements ContainerExecutionService, Consumer<Event<DockerContainerEvent>> {
    private static final Logger log = LoggerFactory.getLogger(HibernateContainerExecutionService.class);

    @Override
    public void accept(final Event<DockerContainerEvent> dockerContainerEventEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Processing docker container event.");
        }
        final DockerContainerEvent event = dockerContainerEventEvent.getData();

        // TODO Check timestamp to make sure we haven't seen this exact event before.
        final ContainerExecution execution = getDao().addEventToHistory(event);

        if (StringUtils.isNotBlank(event.getStatus()) &&
                event.getStatus().matches("kill|die|oom")) {
            finalize(execution);
        }
    }

    @Override
    public void finalize(final ContainerExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Finalizing ContainerExecution for container %s, status %s", execution.getContainerId(), execution.getHistory().get(execution.getHistory().size()-1).getStatus());
        }
        // TODO upload logs

        // TODO upload output files
    }

    @Override
    public ContainerExecution save(final ResolvedCommand resolvedCommand, final String containerId) {
        return null;
    }
}
