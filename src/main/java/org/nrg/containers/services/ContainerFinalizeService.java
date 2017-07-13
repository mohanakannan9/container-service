package org.nrg.containers.services;

import org.nrg.containers.model.container.entity.ContainerEntity;
import org.nrg.xft.security.UserI;

public interface ContainerFinalizeService {
    void finalizeContainer(ContainerEntity containerEntity,
                           UserI userI,
                           String exitCode);
}
