package org.nrg.containers.daos;

import org.nrg.containers.exceptions.NotUniqueException;
import org.nrg.containers.model.DockerHubEntity;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class DockerHubDao extends AbstractHibernateDAO<DockerHubEntity> {
    public DockerHubEntity findByName(final String name) throws NotUniqueException {
        try {
            return findByUniqueProperty("name", name);
        } catch (RuntimeException e) {
            throw new NotUniqueException("More than one result with name " + name + ".");
        }
    }
}
