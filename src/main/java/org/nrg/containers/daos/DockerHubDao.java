package org.nrg.containers.daos;

import org.nrg.containers.model.DockerHubEntity;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

@Repository
public class DockerHubDao extends AbstractHibernateDAO<DockerHubEntity> {
    public DockerHubEntity findByName(final String name) {
        return findByUniqueProperty("name", name);
    }
}
