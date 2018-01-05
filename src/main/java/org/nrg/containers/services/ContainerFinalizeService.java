package org.nrg.containers.services;

import org.nrg.containers.model.container.auto.Container;
import org.nrg.xft.security.UserI;

public interface ContainerFinalizeService {
    Container finalizeContainer(Container toFinalize,
                                UserI userI,
                                boolean isFailed);
}
