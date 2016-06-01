package org.nrg.containers.services;

import org.nrg.containers.exceptions.NotFoundException;
import org.nrg.containers.model.DockerImage;
import org.nrg.containers.model.DockerImageDto;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

public interface DockerImageService extends BaseHibernateService<DockerImage> {
    DockerImage getByDbId(Long id) throws NotFoundException;
    List<DockerImage> getByImageId(String imageId);

    DockerImageDto create(DockerImageDto dto, Boolean onDockerServer);
    DockerImageDto create(DockerImageDto dto);
}
