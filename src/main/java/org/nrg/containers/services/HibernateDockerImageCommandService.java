package org.nrg.containers.services;

import org.nrg.containers.daos.DockerImageCommandDao;
import org.nrg.containers.model.DockerImageCommand;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDockerImageCommandService
        extends AbstractHibernateEntityService<DockerImageCommand, DockerImageCommandDao>
        implements DockerImageCommandService {
}
