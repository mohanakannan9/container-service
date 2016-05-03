package org.nrg.containers.services;

import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.model.Image;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HibernateDockerImageService extends AbstractHibernateEntityService<Image, DockerImageDao>
        implements DockerImageService {
    @Override
    @Transactional
    public List<Image> getByImageId(final String imageId) {
        return getDao().getByImageId(imageId);
    }
}
