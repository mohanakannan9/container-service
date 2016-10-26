package org.nrg.containers.services.impl;

import org.nrg.containers.daos.DockerHubDao;
import org.nrg.containers.model.DockerHub;
import org.nrg.containers.services.DockerHubService;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDockerHubService
        extends AbstractHibernateEntityService<DockerHub, DockerHubDao>
        implements DockerHubService {
}
