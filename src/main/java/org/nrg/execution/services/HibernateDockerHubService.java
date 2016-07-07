package org.nrg.execution.services;

import org.nrg.execution.daos.DockerHubDao;
import org.nrg.execution.model.DockerHub;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDockerHubService
        extends AbstractHibernateEntityService<DockerHub, DockerHubDao>
        implements DockerHubService {
}
