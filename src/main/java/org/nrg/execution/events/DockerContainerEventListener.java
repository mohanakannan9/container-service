package org.nrg.execution.events;

import org.nrg.execution.services.ContainerExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.type;

@Component
public class DockerContainerEventListener implements Consumer<Event<DockerContainerEvent>> {
    private ContainerExecutionService containerExecutionService;

    @Autowired
    public DockerContainerEventListener(final EventBus eventBus) {
        eventBus.on(type(DockerContainerEvent.class), this);
    }

    @Override
    public void accept(final Event<DockerContainerEvent> dockerContainerEventEvent) {
        final DockerContainerEvent event = dockerContainerEventEvent.getData();
        containerExecutionService.processEvent(event);
    }

    @Autowired
    public void setContainerExecutionService(final ContainerExecutionService containerExecutionService) {
        this.containerExecutionService = containerExecutionService;
    }
}
