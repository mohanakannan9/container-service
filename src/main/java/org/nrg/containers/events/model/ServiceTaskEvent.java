package org.nrg.containers.events.model;

import com.google.auto.value.AutoValue;
import org.nrg.containers.model.container.auto.Container;
import org.nrg.containers.model.container.auto.ServiceTask;
import org.nrg.framework.event.EventI;

@AutoValue
public abstract class ServiceTaskEvent implements EventI {
    public abstract ServiceTask task();
    public abstract Container service();

    public static ServiceTaskEvent create(final ServiceTask task, final Container service) {
        return new AutoValue_ServiceTaskEvent(task, service);
    }

}
