package org.nrg.containers.services;

import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.containers.model.auto.DockerHub;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface DockerHubService extends BaseHibernateService<DockerHubEntity> {
    DockerHubEntity retrieve(String name) throws NotUniqueException;
    DockerHubEntity get(String name) throws NotUniqueException, NotFoundException;
    DockerHub retrieveHub(long id);
    DockerHub retrieveHub(String name) throws NotUniqueException;
    DockerHub getHub(long hubId) throws NotFoundException;
    DockerHub getHub(String hubName) throws NotFoundException, NotUniqueException;

    void setDefault(DockerHub dockerHub, String username, String reason);
    DockerHub create(DockerHub dockerHub);
    DockerHubEntity createAndSetDefault(DockerHubEntity dockerHubEntity, String username, String reason);
    DockerHub createAndSetDefault(DockerHub dockerHub, String username, String reason);
    void update(DockerHub dockerHub);
    void updateAndSetDefault(DockerHubEntity dockerHubEntity, String username, String reason);
    void updateAndSetDefault(DockerHub dockerHub, String username, String reason);
    void delete(long id) throws DockerHubDeleteDefaultException;
    void delete(String name) throws DockerHubDeleteDefaultException, NotUniqueException;

    void delete(DockerHubEntity entity) throws DockerHubDeleteDefaultException;
    DockerHub getDefault() throws NotFoundException;


    class DockerHubDeleteDefaultException extends RuntimeException {
        public DockerHubDeleteDefaultException(final String message) {
            super(message);
        }
    }
}
