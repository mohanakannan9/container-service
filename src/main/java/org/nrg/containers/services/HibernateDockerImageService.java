package org.nrg.containers.services;

import org.nrg.containers.daos.DockerImageDao;
import org.nrg.containers.exceptions.NotFoundException;
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

    public DockerImage getByDbId(final Long id) throws NotFoundException {
        final DockerImage dbImage = retrieve(id);
        if (dbImage == null) {
            throw new NotFoundException("No image with id "+id);
        }
        return dbImage;
    }

    @Override
    public List<DockerImage> getByImageId(final String imageId) {
        return getDao().getByImageId(imageId);
    }

    @Transactional
    public DockerImageDto create(DockerImageDto dto) {
        return DockerImageDto.fromDbImage(create(dto.toDbImage()));
    }
}
