package org.nrg.containers.events;

import org.nrg.framework.event.EventI;

import java.util.Date;

public interface ContainerEvent extends EventI {
    String getContainerId();
    String getStatus();
    Date getTime();
}
