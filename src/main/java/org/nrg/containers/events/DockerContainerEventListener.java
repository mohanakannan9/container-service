package org.nrg.containers.events;

import org.nrg.containers.services.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import static reactor.bus.selector.Selectors.type;

@Component
public class DockerContainerEventListener implements Consumer<Event<DockerContainerEvent>> {
    private ContainerService containerService;

    @Autowired
    public DockerContainerEventListener(final EventBus eventBus) {
        eventBus.on(type(DockerContainerEvent.class), this);
    }

    @Override
    public void accept(final Event<DockerContainerEvent> dockerContainerEventEvent) {
        final DockerContainerEvent event = dockerContainerEventEvent.getData();
        containerService.processEvent(event);
    }

    @Autowired
    public void setContainerService(final ContainerService containerService) {
        this.containerService = containerService;
    }
}
