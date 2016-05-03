package org.nrg.containers.daos;

import org.nrg.containers.image.Image;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DockerImageDao extends AbstractHibernateDAO<Image> {
}
