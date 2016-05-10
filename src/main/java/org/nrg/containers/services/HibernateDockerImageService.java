package org.nrg.containers.services;

import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HibernateDockerImageService extends AbstractHibernateEntityService<DockerImage, DockerImageDao>
        implements DockerImageService {
    @Override
    @Transactional
    public List<DockerImage> getByImageId(final String imageId) {
        return getDao().getByImageId(imageId);
    }

    @Transactional
    public DockerImageDto create(DockerImageDto dto) {
        return DockerImageDto.fromDbImage(create(dto.toDbImage()));
    }
}
