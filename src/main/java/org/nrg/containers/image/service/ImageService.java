package org.nrg.containers.image.service;

import org.nrg.containers.image.Image;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

@SuppressWarnings("unused")
public interface ImageService extends BaseHibernateService<Image> {

    List<Image> getByImageId(String imageId);

}
