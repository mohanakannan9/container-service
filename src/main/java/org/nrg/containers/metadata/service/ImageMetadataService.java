package org.nrg.containers.metadata.service;

import org.nrg.containers.metadata.ImageMetadata;
import org.nrg.containers.model.Image;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

@SuppressWarnings("unused")
public interface ImageMetadataService extends BaseHibernateService<ImageMetadata> {

    List<ImageMetadata> getByImageId(String imageId);

    void setMetadata(Image image, ImageMetadata metadata, String project, Boolean overwrite, Boolean ignoreBlank);
}
