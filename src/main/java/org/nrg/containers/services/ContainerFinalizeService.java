package org.nrg.containers.services;

import org.nrg.containers.model.container.auto.Container;
import org.nrg.xft.security.UserI;

import java.util.List;

public interface ContainerFinalizeService {
    Container finalizeContainer(Container toFinalize,
                                UserI userI,
                                boolean isFailed, final List<Container> wrapupContainers);
}
