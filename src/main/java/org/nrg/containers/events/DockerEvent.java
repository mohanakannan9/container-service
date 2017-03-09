package org.nrg.containers.events;

import java.util.Map;

public interface DockerEvent extends ContainerEvent {
    long getTimeNano();
    Map<String, String> getAttributes();
}
