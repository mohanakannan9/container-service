package org.nrg.containers.services;

import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.image.Image;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDockerImageService extends AbstractHibernateEntityService<Image, DockerImageDao>
        implements DockerImageService {
}
