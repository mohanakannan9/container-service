package org.nrg.containers.services;

import org.nrg.containers.model.Image;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

public interface DockerImageService extends BaseHibernateService<Image> {
    List<Image> getByImageId(String imageId);
}
