package org.nrg.containers.services;

import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface DockerHubService extends BaseHibernateService<DockerHubEntity> {
    DockerHub get(long hubId) throws NotFoundException;
    DockerHub get(String hubName);

    DockerHub create(DockerHub dockerHub);

    DockerHub getDefault() throws NotFoundException;
    void setDefault(long hubId, String username, String reason) throws NotFoundException;
    void setDefault(DockerHub dockerHub, String username, String reason);
}
