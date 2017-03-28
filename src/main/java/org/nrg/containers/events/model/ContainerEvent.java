package org.nrg.containers.events.model;

import org.nrg.framework.event.EventI;

import java.util.Date;

public interface ContainerEvent extends EventI {
    String getContainerId();
    String getStatus();
    Date getTime();
}
