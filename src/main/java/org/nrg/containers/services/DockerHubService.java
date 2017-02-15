package org.nrg.containers.services;

import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface DockerHubService extends BaseHibernateService<DockerHubEntity> {
    DockerHub getHub(long hubId) throws NotFoundException;
    DockerHub getHub(String hubName);

    DockerHub create(DockerHub dockerHub);

    void delete(DockerHub dockerHub) throws DockerHubDeleteException;
    void delete(DockerHubEntity entity) throws DockerHubDeleteException;

    DockerHub getDefault() throws NotFoundException;
    void setDefault(DockerHub dockerHub, String username, String reason);

    class DockerHubDeleteException extends RuntimeException {
        public DockerHubDeleteException(final String message) {
            super(message);
        }
    }
}
